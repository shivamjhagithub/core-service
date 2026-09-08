package com.CoreService.CoreService.module.Entities;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "system_modules")
public class ModuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "module_id", nullable = false, updatable = false)
    private String moduleId;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "module_description")
    private String moduleDescription;

    @Column(name = "module_code", nullable = false, unique = true)
    private String moduleCode;
}
