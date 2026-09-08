package com.CoreService.CoreService.module.Services;

import com.CoreService.CoreService.common.exception.ModuleDisabledException;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Repository.CollegeModuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModuleAccessGuardTest {

    private static final UUID COLLEGE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private CollegeModuleRepository collegeModuleRepository;

    @Mock
    private TenantGuard tenantGuard;

    @InjectMocks
    private ModuleAccessGuard moduleAccessGuard;

    @Test
    @DisplayName("a college cannot use a module it has not enabled")
    void rejectsDisabledModule() {
        when(tenantGuard.isMainAdmin()).thenReturn(false);
        when(tenantGuard.requireCollegeId()).thenReturn(COLLEGE_ID);
        when(collegeModuleRepository.existsByCollege_collegeIdAndModule_moduleCodeAndEnabledTrue(
                COLLEGE_ID, ModuleCodes.CHAT)).thenReturn(false);

        assertThatThrownBy(() -> moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT))
                .isInstanceOf(ModuleDisabledException.class)
                .hasMessageContaining(ModuleCodes.CHAT);
    }

    @Test
    void allowsEnabledModule() {
        when(tenantGuard.isMainAdmin()).thenReturn(false);
        when(tenantGuard.requireCollegeId()).thenReturn(COLLEGE_ID);
        when(collegeModuleRepository.existsByCollege_collegeIdAndModule_moduleCodeAndEnabledTrue(
                COLLEGE_ID, ModuleCodes.ATTENDANCE)).thenReturn(true);

        assertThatCode(() -> moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the platform operator is not restricted by per-college module toggles")
    void mainAdminBypassesModuleToggles() {
        when(tenantGuard.isMainAdmin()).thenReturn(true);

        assertThatCode(() -> moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT))
                .doesNotThrowAnyException();

        verify(collegeModuleRepository, never())
                .existsByCollege_collegeIdAndModule_moduleCodeAndEnabledTrue(eq(COLLEGE_ID), eq(ModuleCodes.CHAT));
    }
}
