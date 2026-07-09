package com.CoreService.CoreService.module.Entities;

import com.CoreService.CoreService.College.Entities.CollegeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
@Builder
@Entity
@Getter
@Setter
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "college_id",
                "module_id"
        })
})
public class CollegeModuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private CollegeEntity college;
    @ManyToOne
    private ModuleEntity module;
    private boolean enabled;
}