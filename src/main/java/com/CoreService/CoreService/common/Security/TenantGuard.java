package com.CoreService.CoreService.common.security;

import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Single place where tenant ownership is enforced.
 * <p>
 * Cross-tenant reads are reported as "not found" rather than "forbidden" so a
 * caller cannot use the response to discover that a resource exists in another
 * college.
 */
@Component
public class TenantGuard {

    /**
     * College of the current request, established from the authenticated token.
     */
    public UUID requireCollegeId() {
        UUID collegeId = CollegeContext.getCollegeId();
        if (collegeId == null) {
            throw new UnauthorizedException("No college is associated with the current request");
        }
        return collegeId;
    }

    public UUID currentCollegeId() {
        return CollegeContext.getCollegeId();
    }

    public String requireUserId() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new UnauthorizedException("Request is not authenticated");
        }
        return userId;
    }

    public boolean isMainAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String mainAdmin = "ROLE_" + RoleNames.MAIN_ADMIN;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(mainAdmin::equals);
    }

    /**
     * Rejects any resource that does not belong to the caller's college.
     */
    public void assertSameCollege(UUID resourceCollegeId, String resourceType) {
        if (resourceCollegeId == null) {
            throw new ResourceNotFoundException(resourceType + " is not associated with any college");
        }
        if (isMainAdmin() && CollegeContext.getCollegeId() == null) {
            return;
        }
        if (!resourceCollegeId.equals(requireCollegeId())) {
            throw new ResourceNotFoundException(resourceType, resourceCollegeId);
        }
    }

    public void assertSameCollege(TenantAwareEntity entity, String resourceType) {
        if (entity == null) {
            throw new ResourceNotFoundException(resourceType + " not found");
        }
        assertSameCollege(entity.getCollegeId(), resourceType);
    }

    /**
     * Returns the entity only when it belongs to the caller's college.
     */
    public <T extends TenantAwareEntity> T requireOwned(T entity, String resourceType) {
        assertSameCollege(entity, resourceType);
        return entity;
    }
}
