package com.CoreService.CoreService.audit.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line of the tenant's activity trail: who did what to which resource.
 * <p>
 * There is deliberately no free-form payload column. The trail can therefore
 * never end up holding a password, a token or any other secret, no matter what
 * a calling module passes.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_college_id", columnList = "college_id"),
        @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_logs_action", columnList = "action"),
        @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
})
public class AuditLog extends TenantAwareEntity {

    /** Null for actions triggered by the system rather than by a person. */
    @Column(name = "user_id")
    private String userId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;
}
