package com.CoreService.CoreService.auth.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.Instant;

@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
public class PasswordRequestToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Integer otp;
    private String userId;
    private Instant createdAt;
    private  Instant expiresAt;
    private PasswordRequestToken(){
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plusSeconds(60*3);


    }
}
