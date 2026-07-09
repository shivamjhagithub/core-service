package com.CoreService.CoreService.Permission.Entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
public class PermissionEntity {
    @NonNull
    private String permissionName;
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private String permissionId;
    private String premissionDescription;
}
