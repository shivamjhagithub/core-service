package com.CoreService.CoreService.auth.Services;

import com.CoreService.CoreService.Permission.Repository.PermissionRepository;
import com.CoreService.CoreService.Permission.Repository.RolePermissionRepository;
import com.CoreService.CoreService.auth.Entities.RefreshToken;
import com.CoreService.CoreService.auth.Repositories.RefreshTokenRepo;
import com.CoreService.CoreService.auth.Requests.ChangePasswordRequest;
import com.CoreService.CoreService.auth.Requests.LoginRequests;
import com.CoreService.CoreService.auth.Requests.ResetPasswordRequest;
import com.CoreService.CoreService.auth.Response.LoginResponse;
import com.CoreService.CoreService.common.dto.JwtDto;
import com.CoreService.CoreService.common.jwt.JwtService;
import com.CoreService.CoreService.common.redis.RedisService;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.exception.UnauthorizedException;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.common.security.TokenBlacklistService;
import com.CoreService.CoreService.module.Repository.CollegeModuleRepository;
import com.CoreService.CoreService.module.Repository.ModuleRepository;
import com.CoreService.CoreService.role.Entities.UserRole;
import com.CoreService.CoreService.role.Repository.UserRoleRepository;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Deliberately identical for unknown user and wrong password. */
    private static final String INVALID_CREDENTIALS = "Invalid user id or password";

    private static final String OTP_PREFIX = "OTP:";
    private static final String RESET_PREFIX = "RESET:";
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final Duration RESET_WINDOW = Duration.ofMinutes(10);

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepo userRepository;
    private final CollegeModuleRepository collegeModuleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ModuleRepository moduleRepository;
    private final PermissionRepository permissionRepository;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepo refreshTokenRepo;
    private final RedisService redisService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public LoginResponse login(LoginRequests request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (Boolean.FALSE.equals(user.getActivate())) {
            throw new UnauthorizedException("Account is deactivated");
        }

        return generateLoginResponse(user);
    }

    /**
     * Ends the session on both sides: the refresh token is deleted so no new
     * access token can be minted, and the presented access token is blacklisted
     * so the one already in the client's hands stops working immediately.
     */
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        refreshTokenRepo.findById(refreshToken).ifPresent(refreshTokenRepo::delete);

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                tokenBlacklistService.blacklist(jwtService.extractTokenId(accessToken),
                        jwtService.extractExpiry(accessToken));
            } catch (RuntimeException ex) {
                log.debug("Access token could not be blacklisted on logout", ex);
            }
        }
    }

    /**
     * Rotates the refresh token: the presented one is consumed and a fresh pair
     * is issued, so a leaked token cannot be replayed after use.
     */
    @Transactional
    public LoginResponse updateRefreshTokenAndJwt(String refreshToken) {
        Optional<RefreshToken> stored = refreshTokenRepo.findById(refreshToken);
        if (stored.isEmpty()) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        RefreshToken token = stored.get();
        boolean expired = refreshTokenService.checkExpiration(token);

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        refreshTokenRepo.deleteById(refreshToken);

        if (expired) {
            throw new UnauthorizedException("Refresh token has expired, please log in again");
        }
        if (Boolean.FALSE.equals(user.getActivate())) {
            throw new UnauthorizedException("Account is deactivated");
        }

        return generateLoginResponse(user);
    }

    public void generateOtp(String userId) {
        Optional<UserEntity> user = userRepository.findById(userId);

        // Always succeed from the caller's point of view: telling an anonymous
        // caller whether a user id exists is an account enumeration hole.
        if (user.isEmpty()) {
            log.debug("Password reset requested for unknown user id");
            return;
        }

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        redisService.save(OTP_PREFIX + userId, otp, OTP_EXPIRY);

        log.info("Password reset OTP issued for user {}", userId);
    }

    public boolean checkOtp(String userId, String otp) {
        Object storedOtp = redisService.get(OTP_PREFIX + userId);

        if (storedOtp == null) {
            throw new BusinessException("OTP has expired, request a new one");
        }
        if (!storedOtp.toString().equals(otp)) {
            throw new UnauthorizedException("Invalid OTP");
        }

        redisService.delete(OTP_PREFIX + userId);
        redisService.save(RESET_PREFIX + userId, Boolean.TRUE, RESET_WINDOW);

        return true;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!redisService.exists(RESET_PREFIX + request.getUserId())) {
            throw new UnauthorizedException("OTP verification is required before resetting the password");
        }

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisService.delete(RESET_PREFIX + request.getUserId());
        refreshTokenService.revokeAllForUser(user.getUserId());
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password must differ from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(userId);
    }

    public LoginResponse generateLoginResponse(UserEntity user) {

        UUID collegeId = user.getCollegeId();

        List<UserRole> userRoles = userRoleRepository.findByUser(user);

        List<String> roles = userRoles.stream()
                .map(r -> r.getRole().getRoleName().toUpperCase())
                .distinct()
                .toList();

        boolean isMainAdmin = roles.contains(RoleNames.MAIN_ADMIN);

        List<String> permissions;
        List<String> modules;

        if (isMainAdmin) {
            collegeId = null;
            permissions = permissionRepository.findAll()
                    .stream()
                    .map(permission -> permission.getPermissionCode().toUpperCase())
                    .distinct()
                    .toList();

            modules = moduleRepository.findAll()
                    .stream()
                    .map(module -> module.getModuleCode().toUpperCase())
                    .distinct()
                    .toList();

        } else {
            permissions = userRoles.stream()
                    .flatMap(role -> rolePermissionRepository
                            .findAllByRole_roleId(role.getRole().getRoleId())
                            .stream())
                    .map(rolePermission -> rolePermission.getPermission().getPermissionCode().toUpperCase())
                    .distinct()
                    .toList();

            modules = collegeModuleRepository
                    .findByCollege_collegeIdAndEnabledTrue(collegeId)
                    .stream()
                    .map(collegeModule -> collegeModule.getModule().getModuleCode().toUpperCase())
                    .distinct()
                    .toList();
        }

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user.getUserId());

        JwtDto jwtDto = JwtDto.builder()
                .userId(user.getUserId())
                .collegeId(collegeId)
                .roles(roles)
                .permissions(permissions)
                .modules(modules)
                .build();

        return LoginResponse.builder()
                .jwtToken(jwtService.generateJwtToken(jwtDto))
                .refreshToken(refreshToken.getId())
                .message("Login successful")
                .build();
    }

    /** Housekeeping for expired refresh tokens; safe to call from a scheduler. */
    @Transactional
    public int purgeExpiredRefreshTokens() {
        return refreshTokenRepo.deleteExpired(Instant.now());
    }
}
