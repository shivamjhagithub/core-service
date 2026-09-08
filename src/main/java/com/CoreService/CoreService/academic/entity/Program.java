package com.CoreService.CoreService.academic.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "academic_programs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_programs_college_code",
                columnNames = {"college_id", "code"}),
        indexes = {
                @Index(name = "idx_academic_programs_college_id", columnList = "college_id"),
                @Index(name = "idx_academic_programs_department_id", columnList = "department_id")
        })
public class Program extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "duration_years")
    private Integer durationYears;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "department_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_academic_programs_department"))
    private Department department;
}
