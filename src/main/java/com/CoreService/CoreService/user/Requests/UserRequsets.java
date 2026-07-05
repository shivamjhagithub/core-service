package com.CoreService.CoreService.user.Requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequsets {
    @NotBlank
    private String userid;

    @NotBlank
    private String password;

    @Email
    @NotBlank
    private String email;
}
