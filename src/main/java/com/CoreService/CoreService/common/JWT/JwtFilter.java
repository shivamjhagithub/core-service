package com.CoreService.CoreService.common.JWT;

import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.common.context.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final CollegeContext collegeContext;
    @Autowired
    private  UserContext userContext;

    public JwtFilter(JwtService jwtService,
                     UserDetailsService userDetailsService,CollegeContext collegeContext) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.collegeContext=collegeContext;
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

            // Validate token
            if (!jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract claims
            Claims claims = jwtService.extractAllClaims(token);

            String username = claims.getSubject();

            UUID collegeId = UUID.fromString(
                    claims.get("collegeId", String.class)
            );

            List<String> roles = claims.get("roles", List.class);
            List<String> permissions = claims.get("permissions", List.class);
            List<String> modules = claims.get("modules", List.class);

            // Store college id in ThreadLocal
            if(collegeId!=collegeContext.getCollegeId()){
                throw  new RuntimeException("User not found in this college");
            }
            userContext.builder().userId(username).collegeId(collegeId.toString()).build();
            // Create authorities
            List<GrantedAuthority> authorities = new ArrayList<>();

            if (roles != null) {
                roles.forEach(role ->
                        authorities.add(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        )
                );
            }

            if (permissions != null) {
                permissions.forEach(permission ->
                        authorities.add(
                                new SimpleGrantedAuthority(permission)
                        )
                );
            }

            if (modules != null) {
                modules.forEach(module ->
                        authorities.add(
                                new SimpleGrantedAuthority("MODULE_" + module)
                        )
                );
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails user =
                        userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } finally {
            CollegeContext.clear();
        }
    }
}