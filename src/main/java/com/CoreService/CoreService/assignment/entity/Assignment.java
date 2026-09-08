package com.CoreService.CoreService.assignment.entity;

import com.CoreService.CoreService.assignment.enums.AssignmentStatus;
import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignments", indexes = {
        @Index(name = "idx_assignments_college_id", columnList = "college_id"),
        @Index(name = "idx_assignments_classroom_id", columnList = "classroom_id"),
        @Index(name = "idx_assignments_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Assignment extends TenantAwareEntity {

    @Column(name = "classroom_id", nullable = false)
    private UUID classroomId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "max_marks")
    private Double maxMarks;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;
}
