package com.CoreService.CoreService.announcement.entity;

import com.CoreService.CoreService.announcement.enums.AnnouncementTarget;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An announcement is stored once and read by many; the audience is described by
 * {@code target} plus at most one identifier, so publishing can resolve the
 * recipients at that moment without freezing them into the row.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcements_college_id", columnList = "college_id"),
        @Index(name = "idx_announcements_target", columnList = "target"),
        @Index(name = "idx_announcements_created_at", columnList = "created_at")
})
public class Announcement extends TenantAwareEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "target", nullable = false, length = 20)
    private AnnouncementTarget target;

    /** Department id or classroom id, depending on {@link #target}. */
    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "target_user_id")
    private String targetUserId;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
