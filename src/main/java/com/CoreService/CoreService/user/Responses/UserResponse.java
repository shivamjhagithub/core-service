package com.CoreService.CoreService.user.Responses;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String userId;

    private String fatherName;

    private UUID collegeId;

    private String image;

    private Long phoneNumber;

    private Long alternatePhoneNumber;

    private String bloodGroup;

    private String email;

    private Boolean active;
}
