package com.CoreService.CoreService.academic.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Top of the academic hierarchy: department -> program -> semester -> subject.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "academic_departments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_departments_college_code",
                columnNames = {"college_id", "code"}),
        indexes = @Index(name = "idx_academic_departments_college_id", columnList = "college_id"))
public class Department extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;
}
