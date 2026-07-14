package com.CoreService.CoreService.auth.Services;

import com.CoreService.CoreService.College.Repository.CollegeRepository;
import com.CoreService.CoreService.Permission.Repository.PermissionRepository;
import com.CoreService.CoreService.Permission.Repository.RolePermissionRepository;
import com.CoreService.CoreService.auth.Entities.RefreshToken;
import com.CoreService.CoreService.auth.Repositories.RefreshTokenRepo;
import com.CoreService.CoreService.auth.Requests.LoginRequests;
import com.CoreService.CoreService.auth.Response.LoginResponse;
import com.CoreService.CoreService.common.DTO.JwtDto;
import com.CoreService.CoreService.common.JWT.JwtService;
import com.CoreService.CoreService.module.Entities.CollegeModuleEntity;
import com.CoreService.CoreService.module.Repository.CollegeModuleRepository;
import com.CoreService.CoreService.module.Repository.ModuleRepository;
import com.CoreService.CoreService.role.Entities.Role;
import com.CoreService.CoreService.role.Entities.UserRole;
import com.CoreService.CoreService.role.Repository.UserRoleRepository;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    // private final UserRepository userRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final JwtService jwtService;
    @Autowired
    private final UserRepo userRepository;
    @Autowired
    private final CollegeRepository collegeRepository;
    @Autowired
    private final CollegeModuleRepository collegeModuleRepository;
    @Autowired
    private final UserRoleRepository userRoleRepository;
    @Autowired
    private final RolePermissionRepository rolePermissionRepository;
    @Autowired
    private final ModuleRepository moduleRepository;
    @Autowired
    private final PermissionRepository permissionRepository;
    @Autowired
    private final RefreshTokenService refreshTokenService;
    @Autowired
    private final RefreshTokenRepo  refreshTokenRepo;
    public LoginResponse login(LoginRequests request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid User ID or Password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid User ID or Password");
        }
        return generateLoginResponse(user);
        // Find user
       }

    public Boolean logout(String userId) {
        return false;
    }

    public LoginResponse updateRefreshTokenAndJwt(String refreshToken) {
       Optional< RefreshToken> refreshToken1=refreshTokenRepo.findById(refreshToken);
        if (refreshToken1.isPresent()) {
            Boolean isExpired= refreshTokenService.checkExpiration(refreshToken1.get());
            UserEntity user = userRepository.findById(refreshToken1.get().getUserId())
                    .orElseThrow(() -> new RuntimeException("Invalid User ID or Password"));
            refreshTokenRepo.deleteById(refreshToken);
            if (isExpired) {
                throw new RuntimeException("Login again");
            }
            else return generateLoginResponse(user);
        }
        else {
            throw new RuntimeException("Invalid Refresh Token");
        }
    }

    public void generateOtp(String otp) {
    }

    public boolean checkOtp(String otp) {
        return false;
    }
    public LoginResponse generateLoginResponse(UserEntity user) {

        // Verify password


        UUID collegeId = user.getCollegeId();

        List<UserRole> userRoles = userRoleRepository.findByUser(user);

        List<String> roles = userRoles.stream()
                .map(r -> r.getRole().getRoleName().toUpperCase())
                .distinct()
                .toList();

        boolean isMainAdmin = roles.contains("MAIN_ADMIN");

        List<String> permissions;
        List<String> modules;

        if (isMainAdmin) {

            permissions = permissionRepository.findAll()
                    .stream()
                    .map(permission -> permission.getPermissionName().toUpperCase())
                    .distinct()
                    .toList();

            modules = moduleRepository.findAll()
                    .stream()
                    .map(module -> module.getModuleCode().toUpperCase())
                    .distinct()
                    .toList();

        } else {

            permissions = userRoles.stream()
                    .flatMap(role ->
                            rolePermissionRepository
                                    .findAllByRole_roleId(role.getRole().getRoleId())
                                    .stream()
                    )
                    .map(rolePermission ->
                            rolePermission.getPermission().getPermissionName().toUpperCase()
                    )
                    .distinct()
                    .toList();

            modules = collegeModuleRepository
                    .findByCollege_collegeIdAndEnabledTrue(collegeId)
                    .stream()
                    .map(collegeModule ->
                            collegeModule.getModule().getModuleCode().toUpperCase()
                    )
                    .distinct()
                    .toList();
        }
        RefreshToken refreshToken=refreshTokenService.generateRefreshToken(user.getUserId());
        JwtDto jwtDto = JwtDto.builder()
                .userId(user.getUserId())
                .collegeId(collegeId)
                .roles(roles)
                .permissions(permissions)
                .modules(modules)
                .build();


        String jwtToken = jwtService.generateJwtToken(jwtDto);

        return LoginResponse.builder()
                .jwtToken(jwtToken)
                .refreshToken(refreshToken.getId())
                .message("Login Successful")
                .build();

    }
}
