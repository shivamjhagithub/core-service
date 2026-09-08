package com.CoreService.CoreService.common.websocket;

import com.CoreService.CoreService.common.jwt.JwtService;
import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.common.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Authenticates STOMP CONNECT frames and authorizes SUBSCRIBE frames.
 * <p>
 * The HTTP handshake itself is left open in {@code SecurityConfig}; nothing can
 * be sent or received until a CONNECT frame carries a valid access token. The
 * resulting {@link StompPrincipal} is the only source of caller identity, so a
 * client cannot impersonate another user by putting a different id in a frame
 * body.
 * <p>
 * {@code clientInboundChannel} is an {@code ExecutorSubscribableChannel}:
 * {@code preSend} runs on the container thread while message handlers are
 * dispatched to the channel's executor. Authentication and subscription
 * authorization therefore happen in {@code preSend}, but the tenant context is
 * bound in {@link #beforeHandle} — the only hook that runs on the same thread
 * as the {@code @MessageMapping} method, and thus the only place where
 * {@code TenantGuard} and {@code ModuleAccessGuard} can see it.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ExecutorChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final WebSocketDestinationAuthorizer destinationAuthorizer;

    public WebSocketAuthChannelInterceptor(JwtService jwtService,
                                           TokenBlacklistService tokenBlacklistService,
                                           WebSocketDestinationAuthorizer destinationAuthorizer) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.destinationAuthorizer = destinationAuthorizer;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor));
            return message;
        }

        if (StompCommand.DISCONNECT.equals(command)) {
            return message;
        }

        StompPrincipal principal = principalOf(accessor);

        if (StompCommand.SUBSCRIBE.equals(command)
                && !destinationAuthorizer.isAllowed(accessor.getDestination(), principal)) {
            throw new WebSocketAccessDeniedException(
                    "Not allowed to subscribe to " + accessor.getDestination());
        }

        return message;
    }

    /** Runs on the handler thread, immediately before the message is handled. */
    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && accessor.getUser() instanceof StompPrincipal principal) {
            bindRequestContext(principal);
        }
        return message;
    }

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel,
                                    MessageHandler handler, Exception ex) {
        clearRequestContext();
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        // Defensive: preSend binds nothing, but a handler invoked inline on this
        // thread would otherwise leave the context behind on a pooled thread.
        clearRequestContext();
    }

    private StompPrincipal authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER)) {
            throw new WebSocketAccessDeniedException("Missing bearer token on CONNECT");
        }

        Claims claims;
        try {
            claims = jwtService.extractAllClaims(header.substring(BEARER.length()));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected WebSocket CONNECT with invalid token", ex);
            throw new WebSocketAccessDeniedException("Invalid access token");
        }

        if (claims.getExpiration() == null || claims.getExpiration().toInstant().isBefore(Instant.now())) {
            throw new WebSocketAccessDeniedException("Expired access token");
        }
        if (tokenBlacklistService.isBlacklisted(claims.getId())) {
            throw new WebSocketAccessDeniedException("Session has been terminated");
        }

        List<String> roles = claimAsList(claims, "roles");
        UUID collegeId = parseUuidOrNull(claims.get("collegeId", String.class));

        if (collegeId == null && !roles.contains(RoleNames.MAIN_ADMIN)) {
            throw new WebSocketAccessDeniedException("Token is not associated with a college");
        }

        return new StompPrincipal(claims.getSubject(), collegeId, roles, claimAsList(claims, "permissions"));
    }

    private StompPrincipal principalOf(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof StompPrincipal principal) {
            return principal;
        }
        throw new WebSocketAccessDeniedException("WebSocket session is not authenticated");
    }

    /**
     * Makes the STOMP identity visible to the same services the REST layer
     * uses, so tenant checks behave identically on both transports.
     */
    private void bindRequestContext(StompPrincipal principal) {
        UserContext.setUserId(principal.getUserId());
        UserContext.setCollegeId(principal.getCollegeId());
        CollegeContext.setCollegeId(principal.getCollegeId());

        List<GrantedAuthority> authorities = new ArrayList<>();
        principal.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        principal.getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission)));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal.getUserId(), null, authorities));
    }

    private void clearRequestContext() {
        UserContext.clear();
        CollegeContext.clear();
        SecurityContextHolder.clearContext();
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<String> claimAsList(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::toUpperCase).toList();
        }
        return List.of();
    }
}
