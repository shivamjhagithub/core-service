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
        name = "academic_subjects",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_subjects_college_code",
                columnNames = {"college_id", "code"}),
        indexes = {
                @Index(name = "idx_academic_subjects_college_id", columnList = "college_id"),
                @Index(name = "idx_academic_subjects_semester_id", columnList = "semester_id")
        })
public class Subject extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "semester_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_academic_subjects_semester"))
    private Semester semester;
}
