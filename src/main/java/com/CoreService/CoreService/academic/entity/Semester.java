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
        name = "academic_semesters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_semesters_college_program_number",
                columnNames = {"college_id", "program_id", "number"}),
        indexes = {
                @Index(name = "idx_academic_semesters_college_id", columnList = "college_id"),
                @Index(name = "idx_academic_semesters_program_id", columnList = "program_id"),
                @Index(name = "idx_academic_semesters_academic_session_id", columnList = "academic_session_id")
        })
public class Semester extends TenantAwareEntity {

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "program_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_academic_semesters_program"))
    private Program program;

    /** Optional: a semester template can exist before it is scheduled. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "academic_session_id",
            foreignKey = @ForeignKey(name = "fk_academic_semesters_session"))
    private AcademicSession academicSession;
}
