package com.CoreService.CoreService.meeting.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import com.CoreService.CoreService.meeting.enums.ParticipantRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meeting_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_participants_meeting_user",
                columnNames = {"meeting_id", "user_id"}),
        indexes = {
                @Index(name = "idx_meeting_participants_college_id", columnList = "college_id"),
                @Index(name = "idx_meeting_participants_meeting_id", columnList = "meeting_id"),
                @Index(name = "idx_meeting_participants_user_id", columnList = "user_id")
        })
public class MeetingParticipant extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_meeting_participants_meeting"))
    private Meeting meeting;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 32)
    private ParticipantRole role;

    @Column(name = "joined_at")
    private Instant joinedAt;

    /** Null while the participant is still in the meeting. */
    @Column(name = "left_at")
    private Instant leftAt;
}
