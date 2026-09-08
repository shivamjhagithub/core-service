package com.CoreService.CoreService.assignment.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "submission_attachments", indexes = {
        @Index(name = "idx_submission_attachments_college_id", columnList = "college_id"),
        @Index(name = "idx_submission_attachments_submission_id", columnList = "assignment_submission_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SubmissionAttachment extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_submission_id", nullable = false)
    private AssignmentSubmission submission;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;
}
