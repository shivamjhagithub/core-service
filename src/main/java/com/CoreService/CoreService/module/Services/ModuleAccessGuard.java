package com.CoreService.CoreService.module.Services;

import com.CoreService.CoreService.common.config.CacheConfig;
import com.CoreService.CoreService.common.exception.ModuleDisabledException;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Repository.CollegeModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Gate every module-specific operation goes through: a college only reaches a
 * feature it has explicitly enabled. Owned by the module package because it is
 * the only one allowed to read the college-module tables.
 */
@Service
@RequiredArgsConstructor
public class ModuleAccessGuard {

    private final CollegeModuleRepository collegeModuleRepository;
    private final TenantGuard tenantGuard;

    public void assertModuleEnabled(String moduleCode) {
        if (tenantGuard.isMainAdmin()) {
            return;
        }
        if (!isEnabled(tenantGuard.requireCollegeId(), moduleCode)) {
            throw new ModuleDisabledException(moduleCode);
        }
    }

    @Cacheable(cacheNames = CacheConfig.COLLEGE_MODULES_CACHE, key = "#collegeId + ':' + #moduleCode")
    public boolean isEnabled(UUID collegeId, String moduleCode) {
        return Boolean.TRUE.equals(collegeModuleRepository
                .existsByCollege_collegeIdAndModule_moduleCodeAndEnabledTrue(collegeId, moduleCode));
    }
}
