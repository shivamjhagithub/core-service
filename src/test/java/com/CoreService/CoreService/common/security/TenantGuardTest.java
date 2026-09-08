package com.CoreService.CoreService.common.security;

import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The central multi-tenancy guarantee: a user of one college must never reach
 * another college's data.
 */
class TenantGuardTest {

    private static final UUID COLLEGE_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COLLEGE_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final TenantGuard tenantGuard = new TenantGuard();

    @AfterEach
    void clearContext() {
        CollegeContext.clear();
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a college A user is refused a college B resource")
    void rejectsResourceFromAnotherCollege() {
        authenticateAs("student-a", COLLEGE_A, "ROLE_STUDENT");

        assertThatThrownBy(() -> tenantGuard.assertSameCollege(COLLEGE_B, "Student"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cross-tenant access reports 'not found' so resource existence never leaks")
    void doesNotRevealThatTheResourceExists() {
        authenticateAs("student-a", COLLEGE_A, "ROLE_STUDENT");

        TenantAwareEntity foreign = entityOwnedBy(COLLEGE_B);

        assertThatThrownBy(() -> tenantGuard.assertSameCollege(foreign, "Assignment"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Assignment");
    }

    @Test
    void allowsResourceFromOwnCollege() {
        authenticateAs("student-a", COLLEGE_A, "ROLE_STUDENT");

        assertThatCode(() -> tenantGuard.assertSameCollege(entityOwnedBy(COLLEGE_A), "Assignment"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireOwnedReturnsTheEntityWhenTenantMatches() {
        authenticateAs("teacher-a", COLLEGE_A, "ROLE_TEACHER");

        TenantAwareEntity own = entityOwnedBy(COLLEGE_A);

        assertThat(tenantGuard.requireOwned(own, "Classroom")).isSameAs(own);
    }

    @Test
    @DisplayName("a platform operator without a college may act across tenants")
    void mainAdminWithoutCollegeIsNotTenantBound() {
        authenticateAs("main-admin", null, "ROLE_" + RoleNames.MAIN_ADMIN);

        assertThatCode(() -> tenantGuard.assertSameCollege(COLLEGE_B, "College"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a platform operator scoped to one college is still checked against it")
    void mainAdminScopedToACollegeIsTenantBound() {
        authenticateAs("main-admin", COLLEGE_A, "ROLE_" + RoleNames.MAIN_ADMIN);

        assertThatThrownBy(() -> tenantGuard.assertSameCollege(COLLEGE_B, "Student"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requiresAnAuthenticatedCollege() {
        assertThatThrownBy(tenantGuard::requireCollegeId)
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void requiresAnAuthenticatedUser() {
        assertThatThrownBy(tenantGuard::requireUserId)
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsAResourceWithoutATenant() {
        authenticateAs("student-a", COLLEGE_A, "ROLE_STUDENT");

        assertThatThrownBy(() -> tenantGuard.assertSameCollege((UUID) null, "Student"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void authenticateAs(String userId, UUID collegeId, String... authorities) {
        UserContext.setUserId(userId);
        UserContext.setCollegeId(collegeId);
        CollegeContext.setCollegeId(collegeId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private TenantAwareEntity entityOwnedBy(UUID collegeId) {
        TenantAwareEntity entity = new TenantAwareEntity() {
        };
        entity.setId(UUID.randomUUID());
        entity.setCollegeId(collegeId);
        return entity;
    }
}
