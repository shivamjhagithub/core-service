package com.CoreService.CoreService.common.web;

import com.CoreService.CoreService.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Fixed-window request throttling backed by Redis. Credential endpoints get a
 * tighter budget than the rest of the API.
 * <p>
 * If Redis is unavailable the filter lets traffic through rather than taking
 * the API down with it.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String KEY_PREFIX = "RATE_LIMIT:";
    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.requests-per-minute:300}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.auth-requests-per-minute:20}")
    private int authRequestsPerMinute;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean authEndpoint = request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
        int limit = authEndpoint ? authRequestsPerMinute : requestsPerMinute;

        if (exceedsLimit(buildKey(request, authEndpoint), limit)) {
            tooManyRequests(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String buildKey(HttpServletRequest request, boolean authEndpoint) {
        String bucket = authEndpoint ? "auth" : "api";
        long window = Instant.now().getEpochSecond() / 60;
        return KEY_PREFIX + bucket + ":" + clientIdentifier(request) + ":" + window;
    }

    private boolean exceedsLimit(String key, int limit) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(2));
            }
            return count != null && count > limit;
        } catch (DataAccessException ex) {
            log.warn("Rate limiting disabled for this request; Redis unavailable: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Authenticated callers are throttled per token subject; anonymous callers
     * per client address. The filter runs before authentication, so identity is
     * read straight from the bearer token when present.
     */
    private String clientIdentifier(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return "token:" + Integer.toHexString(authHeader.substring(7).hashCode());
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void tooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"message":"Too many requests","errorCode":"%s","timestamp":"%s","path":"%s"}"""
                .formatted(ErrorCode.RATE_LIMIT_EXCEEDED.name(), Instant.now(), request.getRequestURI()));
    }
}
