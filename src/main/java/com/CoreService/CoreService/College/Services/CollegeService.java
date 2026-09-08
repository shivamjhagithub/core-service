package com.CoreService.CoreService.College.Services;

import com.CoreService.CoreService.College.Dto.CollegeDto;
import com.CoreService.CoreService.College.Dto.CollegeProvisioningResult;
import com.CoreService.CoreService.College.Entities.CollegeEntity;
import com.CoreService.CoreService.College.Repository.CollegeRepository;
import com.CoreService.CoreService.College.Request.CollegeDataRequest;
import com.CoreService.CoreService.Permission.Entities.PermissionEntity;
import com.CoreService.CoreService.Permission.Entities.RolePermissionEntity;
import com.CoreService.CoreService.Permission.Repository.PermissionRepository;
import com.CoreService.CoreService.Permission.Repository.RolePermissionRepository;
import com.CoreService.CoreService.Permission.Services.RolePermissionService;
import com.CoreService.CoreService.common.exception.DuplicateResourceException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.security.DefaultRolePermissions;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.module.Services.ModuleService;
import com.CoreService.CoreService.role.Entities.Role;
import com.CoreService.CoreService.role.Entities.UserRole;
import com.CoreService.CoreService.role.Repository.RoleRepository;
import com.CoreService.CoreService.role.Repository.UserRoleRepository;
import com.CoreService.CoreService.role.Services.RoleService;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import com.CoreService.CoreService.user.Services.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollegeService {

    private static final Logger log = LoggerFactory.getLogger(CollegeService.class);

    private final CollegeRepository collegeRepository;
    private final UserServiceImpl userService;
    private final ModuleService moduleService;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final UserRepo userRepo;
    private final UserRoleRepository userRoleRepo;
    private final RoleRepository roleRepo;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Provisions a tenant in one transaction: the college, its default roles
     * with permissions, its first administrator and its enabled modules. If any
     * step fails nothing is left behind.
     */
    @Transactional
    public CollegeProvisioningResult createCollege(CollegeDataRequest request) {

        if (userRepo.existsByEmail(request.getAdminEmail())) {
            throw new DuplicateResourceException(
                    "A user already exists with email " + request.getAdminEmail());
        }

        CollegeEntity college = collegeRepository.save(CollegeEntity.builder()
                .collegeAddress(request.getCollegeAddress())
                .collegeCity(request.getCollegeCity())
                .collegeCode(request.getCollegeCode())
                .collegeCountry(request.getCollegeCountry())
                .collegeEmail(request.getCollegeEmail())
                .collegeName(request.getCollegeName())
                .collegeDescription(request.getCollegeDescription())
                .collegePhone(request.getCollegePhone())
                .collegeState(request.getCollegeState())
                .collegeZip(request.getCollegeZip())
                .universityCode(request.getUniversityCode())
                .universityName(request.getUniversityName())
                .active(true)
                .build());

        Map<String, Role> roles = createDefaultRoles(college);

        String adminUserId = uniqueAdminUserId(request.getCollegeCode());
        String temporaryPassword = generatePassword();

        UserEntity collegeAdmin = userRepo.save(UserEntity.builder()
                .userId(adminUserId)
                .userName(request.getCollegeName() + " Administrator")
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(temporaryPassword))
                .collegeId(college.getCollegeId())
                .activate(true)
                .build());

        userRoleRepo.save(UserRole.builder()
                .user(collegeAdmin)
                .role(roles.get(RoleNames.COLLEGE_ADMIN))
                .build());

        int enabledModules = moduleService.enableDefaultModules(college,
                DefaultRolePermissions.DEFAULT_COLLEGE_MODULES);

        log.info("Provisioned college {} ({}) with admin {}",
                college.getCollegeName(), college.getCollegeId(), adminUserId);

        return CollegeProvisioningResult.builder()
                .collegeId(college.getCollegeId())
                .collegeCode(college.getCollegeCode())
                .adminUserId(adminUserId)
                .adminEmail(collegeAdmin.getEmail())
                .temporaryPassword(temporaryPassword)
                .enabledModules(enabledModules)
                .build();
    }

    /**
     * Creates COLLEGE_ADMIN, TEACHER and STUDENT for the college so it is
     * usable immediately. The sets are a starting point the college admin can
     * change.
     */
    private Map<String, Role> createDefaultRoles(CollegeEntity college) {

        List<PermissionEntity> catalogue = permissionRepository.findAll();
        Map<String, PermissionEntity> byCode = catalogue.stream()
                .filter(permission -> permission.getPermissionCode() != null)
                .collect(java.util.stream.Collectors.toMap(
                        permission -> permission.getPermissionCode().toUpperCase(Locale.ROOT),
                        permission -> permission,
                        (first, second) -> first));

        Map<String, Role> created = new java.util.LinkedHashMap<>();
        List<RolePermissionEntity> grants = new ArrayList<>();

        DefaultRolePermissions.BY_ROLE.forEach((roleName, permissionCodes) -> {

            Role role = roleRepo.save(Role.builder()
                    .college(college)
                    .roleName(roleName)
                    .roleDescription(roleName.replace('_', ' ') + " of " + college.getCollegeName())
                    .build());

            created.put(roleName, role);

            for (String code : permissionCodes) {
                PermissionEntity permission = byCode.get(code.toUpperCase(Locale.ROOT));
                if (permission != null) {
                    grants.add(RolePermissionEntity.builder()
                            .role(role)
                            .permission(permission)
                            .build());
                }
            }
        });

        rolePermissionRepository.saveAll(grants);
        return created;
    }

    private String uniqueAdminUserId(String collegeCode) {
        String base = collegeCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (base.isEmpty()) {
            base = "COLLEGE";
        }

        String candidate = base + "-ADMIN";
        int suffix = 1;
        while (userRepo.existsByUserId(candidate)) {
            candidate = base + "-ADMIN-" + (++suffix);
        }
        return candidate;
    }

    private String generatePassword() {
        byte[] bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional(readOnly = true)
    public Page<CollegeDto> getAllCollege(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return collegeRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public CollegeDto getCollegeByCollegeId(UUID collegeId) {
        return collegeRepository.findByCollegeId(collegeId)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("College", collegeId));
    }

    @Transactional
    public CollegeDto updateCollegeData(UUID collegeId, CollegeDataRequest request) {

        CollegeEntity college = collegeRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ResourceNotFoundException("College", collegeId));

        if (request.getCollegeAddress() != null) {
            college.setCollegeAddress(request.getCollegeAddress());
        }
        if (request.getCollegeCity() != null) {
            college.setCollegeCity(request.getCollegeCity());
        }
        if (request.getCollegePhone() != null) {
            college.setCollegePhone(request.getCollegePhone());
        }
        if (request.getCollegeState() != null) {
            college.setCollegeState(request.getCollegeState());
        }
        if (request.getCollegeZip() != null) {
            college.setCollegeZip(request.getCollegeZip());
        }
        if (request.getCollegeCountry() != null) {
            college.setCollegeCountry(request.getCollegeCountry());
        }
        if (request.getCollegeEmail() != null) {
            college.setCollegeEmail(request.getCollegeEmail());
        }
        if (request.getCollegeName() != null) {
            college.setCollegeName(request.getCollegeName());
        }
        if (request.getCollegeDescription() != null) {
            college.setCollegeDescription(request.getCollegeDescription());
        }

        return mapToDto(collegeRepository.save(college));
    }

    /**
     * Removes a tenant and everything it owns. Module, role and user data is
     * deleted through the owning services; feature tables are removed by the
     * {@code college_id} foreign keys declared with {@code ON DELETE CASCADE}.
     */
    @Transactional
    public boolean deleteCollegeData(UUID collegeId) {

        if (collegeId == null) {
            return false;
        }
        if (!collegeRepository.existsById(collegeId)) {
            throw new ResourceNotFoundException("College", collegeId);
        }

        moduleService.deleteCollegeModule(collegeId);
        rolePermissionService.deleteAllRolesPermissionOfCollege(collegeId);
        userService.deleteAllUsersOfCollegeByCollegeId(collegeId);
        roleService.deleteAllRolesOfCollege(collegeId);
        collegeRepository.deleteById(collegeId);

        log.info("Deleted college {} and all data owned by it", collegeId);
        return true;
    }

    public CollegeDto mapToDto(CollegeEntity college) {
        return CollegeDto.builder()
                .collegeId(college.getCollegeId())
                .collegeName(college.getCollegeName())
                .collegeCode(college.getCollegeCode())
                .collegeDescription(college.getCollegeDescription())
                .collegeEmail(college.getCollegeEmail())
                .collegePhone(college.getCollegePhone())
                .collegeAddress(college.getCollegeAddress())
                .collegeCity(college.getCollegeCity())
                .collegeState(college.getCollegeState())
                .collegeZip(college.getCollegeZip())
                .collegeCountry(college.getCollegeCountry())
                .universityName(college.getUniversityName())
                .universityCode(college.getUniversityCode())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CollegeDto> searchColleges(String collegeName, String universityName, String city,
                                           int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (collegeName != null && universityName != null) {
            return collegeRepository
                    .findByCollegeNameContainingIgnoreCaseAndUniversityNameContainingIgnoreCase(
                            collegeName, universityName, pageable)
                    .map(this::mapToDto);
        }
        if (collegeName != null) {
            return collegeRepository.findByCollegeNameContainingIgnoreCase(collegeName, pageable)
                    .map(this::mapToDto);
        }
        if (universityName != null) {
            return collegeRepository.findByUniversityNameContainingIgnoreCase(universityName, pageable)
                    .map(this::mapToDto);
        }
        if (city != null) {
            return collegeRepository.findByCollegeCityContainingIgnoreCase(city, pageable)
                    .map(this::mapToDto);
        }

        return collegeRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Optional<CollegeEntity> findEntity(UUID collegeId) {
        return collegeRepository.findByCollegeId(collegeId);
    }
}
