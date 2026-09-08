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

/**
 * Teacher-side material handed out with an assignment. Only the file module's
 * identifier is kept, never a path.
 */
@Entity
@Table(name = "assignment_attachments", indexes = {
        @Index(name = "idx_assignment_attachments_college_id", columnList = "college_id"),
        @Index(name = "idx_assignment_attachments_assignment_id", columnList = "assignment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AssignmentAttachment extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;
}
