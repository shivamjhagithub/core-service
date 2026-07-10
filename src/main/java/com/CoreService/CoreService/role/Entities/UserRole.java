package com.CoreService.CoreService.role.Entities;

import com.CoreService.CoreService.user.Entities.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;
    @ManyToOne
    private UserEntity user;
    @ManyToOne
    private Role role;
}
