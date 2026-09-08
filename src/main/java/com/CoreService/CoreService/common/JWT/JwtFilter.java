package com.CoreService.CoreService.common.jwt;

import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.common.exception.ErrorCode;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.common.security.TokenBlacklistService;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Establishes identity and tenant for the request.
 * <p>
 * The college is taken from the signed token, never from client input. A
 * platform operator ({@link RoleNames#MAIN_ADMIN}) has no college of its own and
 * may therefore target one explicitly with the {@code X-College-Id} header;
 * for every other user that header is ignored.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    /** Honoured only for MAIN_ADMIN, who is not bound to a single tenant. */
    public static final String COLLEGE_OVERRIDE_HEADER = "X-College-Id";

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;
    private final UserRepo userRepo;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtFilter(JwtService jwtService,
                     UserRepo userRepo,
                     TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims;
            try {
                claims = jwtService.extractAllClaims(token);
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Rejected token on {}", request.getRequestURI(), ex);
                unauthorized(request, response, "Invalid or expired access token");
                return;
            }

            if (claims.getExpiration() == null || claims.getExpiration().toInstant().isBefore(Instant.now())) {
                unauthorized(request, response, "Invalid or expired access token");
                return;
            }

            if (tokenBlacklistService.isBlacklisted(claims.getId())) {
                unauthorized(request, response, "Session has been terminated");
                return;
            }

            String userId = claims.getSubject();
            List<String> roles = claimAsList(claims, "roles");
            List<String> permissions = claimAsList(claims, "permissions");
            List<String> modules = claimAsList(claims, "modules");

            boolean isMainAdmin = roles.contains(RoleNames.MAIN_ADMIN);

            UUID collegeId = resolveCollegeId(claims, request, isMainAdmin);
            if (collegeId == null && !isMainAdmin) {
                unauthorized(request, response, "Token is not associated with a college");
                return;
            }

            Optional<UserEntity> user = userRepo.findById(userId);
            if (user.isEmpty()) {
                unauthorized(request, response, "Invalid or expired access token");
                return;
            }
            if (Boolean.FALSE.equals(user.get().getActivate())) {
                unauthorized(request, response, "Account is deactivated");
                return;
            }

            UserContext.setUserId(userId);
            UserContext.setCollegeId(collegeId);
            CollegeContext.setCollegeId(collegeId);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.get(),
                                null,
                                toAuthorities(roles, permissions, modules));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } finally {
            CollegeContext.clear();
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * A tenant user is pinned to the college inside their token. Only a
     * platform operator without a college may select one per request.
     */
    private UUID resolveCollegeId(Claims claims, HttpServletRequest request, boolean isMainAdmin) {
        String fromToken = claims.get("collegeId", String.class);

        if (fromToken != null && !fromToken.isBlank()) {
            return parseUuidOrNull(fromToken);
        }

        if (isMainAdmin) {
            String requested = request.getHeader(COLLEGE_OVERRIDE_HEADER);
            if (requested != null && !requested.isBlank()) {
                return parseUuidOrNull(requested);
            }
        }

        return null;
    }

    private UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<GrantedAuthority> toAuthorities(List<String> roles,
                                                 List<String> permissions,
                                                 List<String> modules) {

        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        modules.forEach(module -> authorities.add(new SimpleGrantedAuthority("MODULE_" + module)));
        return authorities;
    }

    @SuppressWarnings("unchecked")
    private List<String> claimAsList(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::toUpperCase).toList();
        }
        return List.of();
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {

        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"message":"%s","errorCode":"%s","timestamp":"%s","path":"%s"}"""
                .formatted(message, ErrorCode.UNAUTHORIZED.name(), Instant.now(), request.getRequestURI()));
    }
}
