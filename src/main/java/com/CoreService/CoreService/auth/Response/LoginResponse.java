package com.CoreService.CoreService.auth.Response;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    String refreshtoken;
    String jwtToken;
    String messege;
    public LoginResponse(String messege){
        this.messege=messege;
    }
}
