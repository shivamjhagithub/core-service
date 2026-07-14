package com.CoreService.CoreService.auth.Services;

import com.CoreService.CoreService.College.Repository.CollegeRepository;
import com.CoreService.CoreService.Permission.Repository.RolePermissionRepository;
import com.CoreService.CoreService.auth.Requests.LoginRequests;
import com.CoreService.CoreService.auth.Response.LoginResponse;
import com.CoreService.CoreService.common.DTO.JwtDto;
import com.CoreService.CoreService.common.JWT.JwtService;
import com.CoreService.CoreService.module.Entities.CollegeModuleEntity;
import com.CoreService.CoreService.module.Repository.CollegeModuleRepository;
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

import java.util.List;
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

    public LoginResponse login(LoginRequests request) {

        // Find user
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid User ID or Password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid User ID or Password");
        }

        UUID uuid = user.getCollegeId();
        List<UserRole> roles = userRoleRepository.findByUser(user);
        List<String> permissions = roles.stream().map(e -> rolePermissionRepository.findByRole_roleId(e.getRole().getRoleId()).getPermission().getPermissionName().toUpperCase()).collect(Collectors.toUnmodifiableList());
        List<CollegeModuleEntity> collegeModuleEntities = collegeModuleRepository.findByCollege_collegeIdAndEnabledTrue(uuid);
        List<String> collegeModules = collegeModuleEntities.stream().map(e -> e.getModule().getModuleCode().toUpperCase()).collect(Collectors.toUnmodifiableList());
        // Generate JWT
        JwtDto jwtDto = JwtDto.builder().userId(user.getUserId()).collegeId(uuid).roles(roles.stream().map(e -> e.getRole().getRoleName().toUpperCase())
                .collect(Collectors.toUnmodifiableList())).permissions(permissions).modules(collegeModules).build();
        String jwtToken = jwtService.generateJwtToken(jwtDto);


        return LoginResponse.builder()
                .jwtToken(jwtToken)
                .refreshToken(null) // or generate one if you implement refresh tokens
                .message("Login Successful")
                .build();
    }

    public Boolean logout(String userId) {
        return false;
    }

    public LoginResponse updateRefreshTokenAndJwt(String refreshToken) {
        return null;
    }

    public void generateOtp(String otp) {
    }

    public boolean checkOtp(String otp) {
        return false;
    }

}
