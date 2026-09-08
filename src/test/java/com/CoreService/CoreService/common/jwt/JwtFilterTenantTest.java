package com.CoreService.CoreService.common.jwt;

import com.CoreService.CoreService.common.dto.JwtDto;
import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.common.security.TokenBlacklistService;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The tenant of a request comes from the signed token, not from client input.
 */
class JwtFilterTenantTest {

    private static final UUID COLLEGE_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COLLEGE_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private JwtService jwtService;
    private UserRepo userRepo;
    private TokenBlacklistService tokenBlacklistService;
    private JwtFilter jwtFilter;

    /** Captures the contexts before the filter's finally block clears them. */
    private UUID observedCollegeId;
    private String observedUserId;
    private FilterChain capturingChain;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "unit-test-signing-key-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtService, "accessExpirationMillis", 900_000L);

        userRepo = mock(UserRepo.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);

        jwtFilter = new JwtFilter(jwtService, userRepo, tokenBlacklistService);

        observedCollegeId = null;
        observedUserId = null;
        capturingChain = (req, res) -> {
            observedCollegeId = CollegeContext.getCollegeId();
            observedUserId = UserContext.getUserId();
        };
    }

    @AfterEach
    void tearDown() {
        CollegeContext.clear();
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("the college comes from the token")
    void takesCollegeFromToken() throws Exception {
        activeUser("teacher-a", COLLEGE_A);

        MockHttpServletRequest request = authenticatedRequest("teacher-a", COLLEGE_A, "TEACHER");
        jwtFilter.doFilter(request, new MockHttpServletResponse(), capturingChain);

        assertThat(observedUserId).isEqualTo("teacher-a");
        assertThat(observedCollegeId).isEqualTo(COLLEGE_A);
    }

    @Test
    @DisplayName("a client-supplied X-College-Id header cannot change a tenant user's college")
    void ignoresCollegeHeaderForTenantUsers() throws Exception {
        activeUser("teacher-a", COLLEGE_A);

        MockHttpServletRequest request = authenticatedRequest("teacher-a", COLLEGE_A, "TEACHER");
        request.addHeader(JwtFilter.COLLEGE_OVERRIDE_HEADER, COLLEGE_B.toString());

        jwtFilter.doFilter(request, new MockHttpServletResponse(), capturingChain);

        assertThat(observedCollegeId).isEqualTo(COLLEGE_A);
    }

    @Test
    @DisplayName("a platform operator may target a college explicitly")
    void allowsCollegeHeaderForMainAdmin() throws Exception {
        activeUser("main-admin", null);

        MockHttpServletRequest request = authenticatedRequest("main-admin", null, RoleNames.MAIN_ADMIN);
        request.addHeader(JwtFilter.COLLEGE_OVERRIDE_HEADER, COLLEGE_B.toString());

        jwtFilter.doFilter(request, new MockHttpServletResponse(), capturingChain);

        assertThat(observedCollegeId).isEqualTo(COLLEGE_B);
    }

    @Test
    @DisplayName("a revoked access token is rejected")
    void rejectsBlacklistedToken() throws Exception {
        activeUser("teacher-a", COLLEGE_A);
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(authenticatedRequest("teacher-a", COLLEGE_A, "TEACHER"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("a deactivated account cannot use an already issued token")
    void rejectsDeactivatedAccount() throws Exception {
        when(userRepo.findById("teacher-a")).thenReturn(Optional.of(UserEntity.builder()
                .userId("teacher-a")
                .userName("Teacher A")
                .email("teacher-a@college-a.test")
                .password("hash")
                .collegeId(COLLEGE_A)
                .activate(false)
                .build()));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(authenticatedRequest("teacher-a", COLLEGE_A, "TEACHER"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("a request without a bearer token passes through unauthenticated")
    void passesThroughAnonymousRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("the request contexts are cleared once the request completes")
    void clearsThreadLocalsAfterTheRequest() throws Exception {
        activeUser("teacher-a", COLLEGE_A);

        jwtFilter.doFilter(authenticatedRequest("teacher-a", COLLEGE_A, "TEACHER"),
                new MockHttpServletResponse(), capturingChain);

        assertThat(CollegeContext.getCollegeId()).isNull();
        assertThat(UserContext.getUserId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void activeUser(String userId, UUID collegeId) {
        when(userRepo.findById(userId)).thenReturn(Optional.of(UserEntity.builder()
                .userId(userId)
                .userName(userId)
                .email(userId + "@college.test")
                .password("hash")
                .collegeId(collegeId)
                .activate(true)
                .build()));
    }

    private MockHttpServletRequest authenticatedRequest(String userId, UUID collegeId, String role) {
        String token = jwtService.generateJwtToken(JwtDto.builder()
                .userId(userId)
                .collegeId(collegeId)
                .roles(List.of(role))
                .permissions(List.of())
                .modules(List.of())
                .build());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/classrooms");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
