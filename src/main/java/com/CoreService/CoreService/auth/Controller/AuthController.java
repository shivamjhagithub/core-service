package com.CoreService.CoreService.auth.Controller;

import com.CoreService.CoreService.auth.Requests.*;
import com.CoreService.CoreService.auth.Response.LoginResponse;
import com.CoreService.CoreService.auth.Services.AuthService;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.common.exception.UnauthorizedException;
import com.CoreService.CoreService.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token lifecycle and password management")
public class AuthController {

    private static final String BEARER = "Bearer ";

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for an access and refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequests loginRequest) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(loginRequest)));
    }

    @PostMapping({"/refresh", "/refreshToken"})
    @Operation(summary = "Rotate a refresh token into a new token pair")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed",
                authService.updateRefreshTokenAndJwt(request.getRefreshToken())));
    }

    @RequestMapping(path = "/logout", method = {RequestMethod.POST, RequestMethod.PUT})
    @Operation(summary = "Revoke the refresh token and blacklist the current access token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        authService.logout(request.getRefreshToken(), bearerToken(authorization));
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully"));
    }

    @PostMapping({"/forgot-password", "/forgotPasswordAndSendOtp"})
    @Operation(summary = "Send a password reset OTP",
            description = "Always reports success so that user ids cannot be enumerated.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.generateOtp(request.getUserId());
        return ResponseEntity.ok(ApiResponse.message("If the account exists, an OTP has been sent"));
    }

    @PostMapping({"/verify-otp", "/verifyOtp"})
    @Operation(summary = "Verify a password reset OTP")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody OtpRequest otpRequest) {
        authService.checkOtp(otpRequest.getUserId(), otpRequest.getOtp());
        return ResponseEntity.ok(ApiResponse.message("OTP verified successfully"));
    }

    @PostMapping({"/reset-password", "/resetPassword"})
    @Operation(summary = "Set a new password after OTP verification")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.message("Password reset successfully"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the password of the authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new UnauthorizedException("Request is not authenticated");
        }

        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.message("Password changed successfully"));
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
            return null;
        }
        return authorizationHeader.substring(BEARER.length());
    }
}
