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

import java.time.LocalDate;

/**
 * An academic year such as "2025-2026". At most one session per college carries
 * {@code current}; {@code AcademicService} is what keeps that invariant.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "academic_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_sessions_college_name",
                columnNames = {"college_id", "name"}),
        indexes = @Index(name = "idx_academic_sessions_college_id", columnList = "college_id"))
public class AcademicSession extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private boolean current;
}
