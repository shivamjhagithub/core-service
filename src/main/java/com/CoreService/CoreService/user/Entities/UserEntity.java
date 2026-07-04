package com.CoreService.CoreService.user.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String userId;
    private String fatherName;
    private UUID collegeId;
    private String image;
    private Long phoneNumber;
    private Long alternatePhoneNumber;
    private String bloodGroup;
    private String email;
    private String password;

}
