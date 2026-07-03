package com.CoreService.CoreService.auth.Controller;

import com.CoreService.CoreService.auth.Requests.LoginRequests;
import com.CoreService.CoreService.auth.Response.LoginResponse;
import com.CoreService.CoreService.auth.Services.AuthService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
   @Autowired
   private AuthService authService;

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

}
