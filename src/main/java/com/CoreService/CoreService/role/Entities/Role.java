package com.CoreService.CoreService.role.Entities;

import com.CoreService.CoreService.College.Entities.CollegeEntity;
import com.CoreService.CoreService.common.entity.AuditableEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "roles", indexes = {
        @Index(name = "idx_roles_college_id", columnList = "college_id")
})
public class Role extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "role_description")
    private String roleDescription;

    /** Null for platform-wide roles such as MAIN_ADMIN. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", foreignKey = @ForeignKey(name = "fk_roles_college"))
    @Nullable
    private CollegeEntity college;
}
