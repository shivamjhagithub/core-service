package com.CoreService.CoreService.College.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CollegeDataRequest {

    @NotBlank(message = "College name is required")
    @Size(max = 200)
    private String collegeName;

    @NotBlank(message = "College code is required")
    @Size(max = 50)
    private String collegeCode;

    @Size(max = 20)
    private String collegePhone;

    @NotBlank(message = "College email is required")
    @Email(message = "College email must be a valid email address")
    private String collegeEmail;

    private String collegeAddress;
    private String collegeCity;
    private String collegeState;
    private String collegeZip;
    private String collegeCountry;

    @NotBlank(message = "University name is required")
    private String universityName;

    @NotBlank(message = "University code is required")
    private String universityCode;

    @Size(max = 2000)
    private String collegeDescription;

    /** Mailbox of the first college administrator created with the college. */
    @NotBlank(message = "Administrator email is required")
    @Email(message = "Administrator email must be a valid email address")
    private String adminEmail;
}
