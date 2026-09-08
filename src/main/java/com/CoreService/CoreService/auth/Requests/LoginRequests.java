package com.CoreService.CoreService.auth.Requests;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequests {

    @NotBlank(message = "User id is required")
    private String userId;

    @NotBlank(message = "Password is required")
    private String password;
}
