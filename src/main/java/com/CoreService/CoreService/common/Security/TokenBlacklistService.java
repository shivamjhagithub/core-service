package com.CoreService.CoreService.common.security;

import com.CoreService.CoreService.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Revokes still-valid access tokens on logout. Entries expire together with the
 * token itself, so the blacklist never grows without bound.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "TOKEN_BLACKLIST:";

    private final RedisService redisService;

    public void blacklist(String tokenId, Instant expiresAt) {
        if (tokenId == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisService.save(PREFIX + tokenId, Boolean.TRUE, ttl);
    }

    public boolean isBlacklisted(String tokenId) {
        return tokenId != null && redisService.exists(PREFIX + tokenId);
    }
}
