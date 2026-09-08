package com.CoreService.CoreService.notification.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import com.CoreService.CoreService.notification.enums.NotificationType;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per recipient: a fan-out to thirty students is thirty rows, which is
 * what makes "mark as read" and the unread badge a per-user operation.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_college_id", columnList = "college_id"),
        @Index(name = "idx_notifications_recipient_user_id", columnList = "recipient_user_id"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
public class Notification extends TenantAwareEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private String recipientUserId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", length = 1000)
    private String body;

    /** Domain object the notification points at, e.g. an assignment id. */
    @Column(name = "reference_id")
    private UUID referenceId;

    /** Column is {@code is_read}: READ is a reserved word in MySQL. */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
