package com.CoreService.CoreService.common.jwt;

import com.CoreService.CoreService.common.dto.JwtDto;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final UUID COLLEGE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "unit-test-signing-key-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtService, "accessExpirationMillis", 900_000L);
    }

    @Test
    @DisplayName("the token carries identity, tenant and authority claims")
    @SuppressWarnings("unchecked") // Claims.get(String, Class) cannot express List<String>
    void encodesIdentityAndTenant() {
        String token = jwtService.generateJwtToken(JwtDto.builder()
                .userId("teacher-1")
                .collegeId(COLLEGE_ID)
                .roles(List.of("TEACHER"))
                .permissions(List.of("CLASSROOM_CREATE"))
                .modules(List.of("CLASSROOM"))
                .build());

        Claims claims = jwtService.extractAllClaims(token);

        List<String> roles = claims.get("roles", List.class);
        List<String> permissions = claims.get("permissions", List.class);

        assertThat(claims.getSubject()).isEqualTo("teacher-1");
        assertThat(claims.get("collegeId", String.class)).isEqualTo(COLLEGE_ID.toString());
        assertThat(roles).containsExactly("TEACHER");
        assertThat(permissions).containsExactly("CLASSROOM_CREATE");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("every token gets an id so it can be revoked before expiry")
    void assignsARevocableTokenId() {
        String first = jwtService.generateJwtToken(jwtDto());
        String second = jwtService.generateJwtToken(jwtDto());

        assertThat(jwtService.extractTokenId(first)).isNotBlank();
        assertThat(jwtService.extractTokenId(first)).isNotEqualTo(jwtService.extractTokenId(second));
        assertThat(jwtService.extractExpiry(first)).isNotNull();
    }

    @Test
    void rejectsAnExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessExpirationMillis", -1_000L);

        assertThat(jwtService.isTokenValid(jwtService.generateJwtToken(jwtDto()))).isFalse();
    }

    @Test
    @DisplayName("a token signed with a different key is rejected")
    void rejectsAForeignSignature() {
        String token = jwtService.generateJwtToken(jwtDto());

        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret",
                "a-completely-different-signing-key-also-long-enough");
        ReflectionTestUtils.setField(otherService, "accessExpirationMillis", 900_000L);

        assertThat(otherService.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> otherService.extractAllClaims(token)).isInstanceOf(Exception.class);
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwtService.isTokenValid("not-a-token")).isFalse();
    }

    private JwtDto jwtDto() {
        return JwtDto.builder()
                .userId("teacher-1")
                .collegeId(COLLEGE_ID)
                .roles(List.of("TEACHER"))
                .permissions(List.of())
                .modules(List.of())
                .build();
    }
}
