package com.CoreService.CoreService.auth.Services;

import com.CoreService.CoreService.auth.Entities.RefreshToken;
import com.CoreService.CoreService.auth.Repositories.RefreshTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMillis;

    @Transactional
    public RefreshToken generateRefreshToken(String userId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMillis))
                .build();

        return refreshTokenRepo.save(refreshToken);
    }

    /**
     * Deletes the token when it has expired.
     *
     * @return true when the token was expired and is no longer usable
     */
    @Transactional
    public boolean checkExpiration(RefreshToken refreshToken) {
        if (Instant.now().isAfter(refreshToken.getExpiresAt())) {
            refreshTokenRepo.deleteById(refreshToken.getId());
            return true;
        }
        return false;
    }

    /** Invalidates every session of a user, e.g. after a password change. */
    @Transactional
    public void revokeAllForUser(String userId) {
        refreshTokenRepo.deleteAllByUserId(userId);
    }
}
