package com.CoreService.CoreService.College.Dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Returned once, to the platform operator who created the college. The
 * temporary password is never stored in clear text and never retrievable
 * again — the college admin must change it on first login.
 */
@Builder
public record CollegeProvisioningResult(UUID collegeId,
                                        String collegeCode,
                                        String adminUserId,
                                        String adminEmail,
                                        String temporaryPassword,
                                        int enabledModules) {
}
