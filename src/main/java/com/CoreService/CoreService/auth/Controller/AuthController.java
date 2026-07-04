package com.CoreService.CoreService.auth.Controller;

import com.CoreService.CoreService.auth.Requests.LoginRequests;
import com.CoreService.CoreService.auth.Response.LoginResponse;
import com.CoreService.CoreService.auth.Services.AuthService;
import com.CoreService.CoreService.common.context.UserContext;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
   @Autowired
   private AuthService authService;
   @Autowired
   private UserContext userContext;

   @PostMapping("/login")
   public ResponseEntity<LoginResponse> login(@RequestBody LoginRequests loginRequest) {
      try{
         LoginResponse loginResponse=authService.login(loginRequest);
         if(loginResponse==null)
         {return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                 .body(new LoginResponse("Invalid username or password"));
         }
         return ResponseEntity.ok(loginResponse);
      }
      catch (Exception e){
         return ResponseEntity.badRequest().body(new LoginResponse("Invalid username or password"));
      }
   }
   @PutMapping("/logout")
   public ResponseEntity<?> logout() {
      boolean active=authService.logout(userContext.getUserId());
      if(active==false){
         ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("logout unsuccessful"));
      }
      return ResponseEntity.ok("logout successful");
   }
   @PostMapping("/refreshToken")
   public ResponseEntity<LoginResponse> updateRefreshTokenAndJwt(@RequestBody String refreshToken){
      LoginResponse ans = authService.updateRefreshTokenAndJwt(refreshToken);
      return new ResponseEntity<>(ans, HttpStatus.OK);
   }
   @PostMapping("/forgotPasswordAndSendOtp")
   public ResponseEntity<?> forgotPasswordAndSendOtp(@RequestBody String email){
      return ResponseEntity.ok("forgot password");
   }
   @GetMapping("/verifyOtp")
   public ResponseEntity<?> verifyOtp(@RequestBody String otp){
      return ResponseEntity.ok(" otp verified");
   }

}