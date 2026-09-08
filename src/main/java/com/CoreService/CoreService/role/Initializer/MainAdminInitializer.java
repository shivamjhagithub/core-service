package com.CoreService.CoreService.role.Initializer;


import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.role.Entities.Role;
import com.CoreService.CoreService.role.Entities.UserRole;
import com.CoreService.CoreService.role.Repository.RoleRepository;
import com.CoreService.CoreService.role.Repository.UserRoleRepository;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Creates the platform operator account on a fresh database.
 * <p>
 * The password is never hardcoded: supply {@code BOOTSTRAP_ADMIN_PASSWORD}, or
 * a random one is generated and printed once so it can be rotated on first
 * login.
 */
@Component
@RequiredArgsConstructor
@Order(3)
public class MainAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MainAdminInitializer.class);

    private final UserRepo userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.user-id}")
    private String adminUserId;

    @Value("${app.bootstrap.admin.email}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (userRepository.existsByUserId(adminUserId) || userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role mainAdminRole = roleRepository
                .findByRoleNameAndCollegeIsNull(RoleNames.MAIN_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        RoleNames.MAIN_ADMIN + " role was not initialized"));

        boolean generated = adminPassword == null || adminPassword.isBlank();
        String password = generated ? generatePassword() : adminPassword;

        UserEntity admin = userRepository.save(UserEntity.builder()
                .userId(adminUserId)
                .userName("Main Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(password))
                .activate(true)
                .build());

        userRoleRepository.save(UserRole.builder()
                .user(admin)
                .role(mainAdminRole)
                .build());

        if (generated) {
            log.warn("""
                    Created platform admin '{}' with a generated password: {}
                    Set BOOTSTRAP_ADMIN_PASSWORD and change this password immediately.""",
                    adminUserId, password);
        } else {
            log.info("Created platform admin '{}'", adminUserId);
        }
    }

    private String generatePassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
