package com.CoreService.CoreService.module.Entities;

import com.CoreService.CoreService.College.Entities.CollegeEntity;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "college_modules",
        uniqueConstraints = @UniqueConstraint(name = "uk_college_modules_college_module",
                columnNames = {"college_id", "module_id"}),
        indexes = @Index(name = "idx_college_modules_college_id", columnList = "college_id"))
public class CollegeModuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_college_modules_college"))
    private CollegeEntity college;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_college_modules_module"))
    private ModuleEntity module;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;
}
