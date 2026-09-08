package com.CoreService.CoreService.College.Entities;

import com.CoreService.CoreService.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.lang.NonNull;

import java.util.UUID;

@Builder
@Setter
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "colleges")
public class CollegeEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "college_id", nullable = false, updatable = false)
    private UUID collegeId;

    @Column(name = "college_name", nullable = false)
    private String collegeName;

    @Column(name = "college_code", nullable = false, unique = true)
    @NonNull
    private String collegeCode;

    @Column(name = "college_phone")
    private String collegePhone;

    @Column(name = "college_email", nullable = false, unique = true)
    @NonNull
    private String collegeEmail;

    @Column(name = "college_address")
    private String collegeAddress;

    @Column(name = "college_city")
    private String collegeCity;

    @Column(name = "college_state")
    private String collegeState;

    @Column(name = "college_zip")
    private String collegeZip;

    @Column(name = "college_country")
    private String collegeCountry;

    @Column(name = "university_name", nullable = false)
    @NonNull
    private String universityName;

    @Column(name = "university_code", nullable = false)
    @NonNull
    private String universityCode;

    @Column(name = "college_description", length = 2000)
    private String collegeDescription;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
