package com.CoreService.CoreService.College.Request;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class CollegeDataRequest {
    private String collegeName;
    private String collegeCode;
    private String collegePhone;
    private String collegeEmail;
    private String collegeAddress;
    private String collegeCity;
    private String collegeState;
    private String collegeZip;
    private String collegeCountry;
    private String universityName;
    private String universityCode;
    private String collegeDescription;
    private String adminEmail;
}
