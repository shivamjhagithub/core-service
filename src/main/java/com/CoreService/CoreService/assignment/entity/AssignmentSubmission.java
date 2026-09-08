package com.CoreService.CoreService.assignment.entity;

import com.CoreService.CoreService.assignment.enums.SubmissionStatus;
import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "assignment_submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assignment_submissions_assignment_student",
                columnNames = {"assignment_id", "student_user_id"}),
        indexes = {
                @Index(name = "idx_assignment_submissions_college_id", columnList = "college_id"),
                @Index(name = "idx_assignment_submissions_student_user_id", columnList = "student_user_id"),
                @Index(name = "idx_assignment_submissions_assignment_id", columnList = "assignment_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class AssignmentSubmission extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "student_user_id", nullable = false, length = 64)
    private String studentUserId;

    @Column(name = "content", length = 4000)
    private String content;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    @Column(name = "marks")
    private Double marks;

    @Column(name = "feedback", length = 2000)
    private String feedback;

    @Column(name = "graded_by", length = 64)
    private String gradedBy;

    @Column(name = "graded_at")
    private Instant gradedAt;
}
