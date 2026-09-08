package com.CoreService.CoreService.teacher.entity;

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

import java.util.UUID;

/**
 * Subject a teacher is assigned to teach. {@code subjectId} is validated through
 * the academic module's lookup API rather than a JPA association, so the two
 * modules share no entity graph.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "teacher_subjects",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_teacher_subjects_college_teacher_subject",
                columnNames = {"college_id", "teacher_id", "subject_id"}),
        indexes = {
                @Index(name = "idx_teacher_subjects_college_id", columnList = "college_id"),
                @Index(name = "idx_teacher_subjects_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_teacher_subjects_subject_id", columnList = "subject_id")
        })
public class TeacherSubject extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "teacher_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_teacher_subjects_teacher"))
    private Teacher teacher;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;
}
