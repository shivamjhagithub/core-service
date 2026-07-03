package com.CoreService.CoreService.auth.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    String refreshtoken;
    String jwtToken;
    String messege;
    public LoginResponse(String messege){
        this.messege=messege;
    }
}
