package com.CoreService.CoreService.role.Initializer;

import com.CoreService.CoreService.Permission.Entities.PermissionEntity;
import com.CoreService.CoreService.Permission.Entities.RolePermissionEntity;
import com.CoreService.CoreService.Permission.Repository.PermissionRepository;
import com.CoreService.CoreService.Permission.Repository.RolePermissionRepository;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.role.Entities.Role;
import com.CoreService.CoreService.role.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Grants the platform-wide MAIN_ADMIN role every permission in the catalogue.
 */
@Component
@RequiredArgsConstructor
@Order(2)
public class Initializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Initializer.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public void run(String... args) {

        Role superAdminRole;

        if (roleRepository.existsByRoleNameAndCollegeIsNull(RoleNames.MAIN_ADMIN)) {

            superAdminRole = roleRepository
                    .findByRoleNameAndCollegeIsNull(RoleNames.MAIN_ADMIN)
                    .orElseThrow();

        } else {

            superAdminRole = Role.builder()
                    .roleName(RoleNames.MAIN_ADMIN)
                    .roleDescription("Platform Super Admin")
                    .college(null)
                    .build();

            superAdminRole = roleRepository.save(superAdminRole);
        }

        List<PermissionEntity> permissions = permissionRepository.findAll();

        int granted = 0;
        for (PermissionEntity permission : permissions) {

            if (!rolePermissionRepository.existsByRole_roleIdAndPermission_permissionId(
                    superAdminRole.getRoleId(),
                    permission.getPermissionId())) {

                rolePermissionRepository.save(
                        RolePermissionEntity.builder()
                                .role(superAdminRole)
                                .permission(permission)
                                .build()
                );
                granted++;
            }
        }

        if (granted > 0) {
            log.info("Granted {} additional permissions to {}", granted, RoleNames.MAIN_ADMIN);
        }
    }
}