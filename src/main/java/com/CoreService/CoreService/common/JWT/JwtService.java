package com.CoreService.CoreService.common.jwt;


import com.CoreService.CoreService.common.dto.JwtDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpirationMillis;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {

        try {

            Claims claims = extractAllClaims(token);

            return claims.getExpiration().after(new Date());

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Access token. Claims stay minimal: identity, tenant and coarse-grained
     * authority. Anything larger is resolved server side per request.
     */
    public String generateJwtToken(JwtDto jwtDto) {

        Instant now = Instant.now();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(jwtDto.getUserId())
                .claim("collegeId", jwtDto.getCollegeId())
                .claim("roles", jwtDto.getRoles())
                .claim("permissions", jwtDto.getPermissions())
                .claim("modules", jwtDto.getModules())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMillis)))
                .signWith(getSigningKey())
                .compact();
    }

    /** Identifier used to revoke a token before it expires. */
    public String extractTokenId(String token) {
        return extractAllClaims(token).getId();
    }

    public Instant extractExpiry(String token) {
        return extractAllClaims(token).getExpiration().toInstant();
    }
}
