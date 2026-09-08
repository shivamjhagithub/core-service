package com.CoreService.CoreService.user.Entities;

import com.CoreService.CoreService.common.entity.AuditableEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_college_id", columnList = "college_id"),
        @Index(name = "idx_users_email", columnList = "email")
})
public class UserEntity extends AuditableEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @NonNull
    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "father_name")
    private String fatherName;

    /** Null only for MAIN_ADMIN, who operates above any single college. */
    @Nullable
    @Column(name = "college_id")
    private UUID collegeId;

    @Column(name = "image")
    private String image;

    @Column(name = "phone_number")
    private Long phoneNumber;

    @Column(name = "alternate_phone_number")
    private Long alternatePhoneNumber;

    @Column(name = "blood_group", length = 8)
    private String bloodGroup;

    @NonNull
    @Column(name = "email", nullable = false)
    private String email;

    /** BCrypt hash. Never serialized to a DTO. */
    @NonNull
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "activate")
    private Boolean activate;
}
