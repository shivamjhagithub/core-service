package com.CoreService.CoreService.auth.Services;

import com.CoreService.CoreService.auth.Requests.LoginRequests;
import com.CoreService.CoreService.auth.Response.LoginResponse;
import com.CoreService.CoreService.common.DTO.JwtDto;
import com.CoreService.CoreService.common.JWT.JwtService;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public LoginResponse login(LoginRequests request) {

        // Find user
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid User ID or Password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid User ID or Password");
        }

        // Generate JWT
        JwtDto jwtDto=JwtDto.builder().userId(user.getUserId()).collegeId()
        String jwtToken = jwtService.generateJwtToken();


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
