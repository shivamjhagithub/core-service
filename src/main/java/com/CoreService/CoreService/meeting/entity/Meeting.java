package com.CoreService.CoreService.meeting.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import com.CoreService.CoreService.meeting.enums.MeetingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meetings",
        indexes = {
                @Index(name = "idx_meetings_college_id", columnList = "college_id"),
                @Index(name = "idx_meetings_classroom_id", columnList = "classroom_id"),
                @Index(name = "idx_meetings_status", columnList = "status"),
                @Index(name = "idx_meetings_scheduled_start_time", columnList = "scheduled_start_time"),
                @Index(name = "idx_meetings_college_status_start",
                        columnList = "college_id, status, scheduled_start_time")
        })
public class Meeting extends TenantAwareEntity {

    /** Null for ad-hoc meetings that are not tied to a classroom. */
    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "host_user_id", nullable = false)
    private String hostUserId;

    @Column(name = "scheduled_start_time", nullable = false)
    private Instant scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private Instant scheduledEndTime;

    @Column(name = "actual_start_time")
    private Instant actualStartTime;

    @Column(name = "actual_end_time")
    private Instant actualEndTime;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 32)
    private MeetingStatus status;

    /**
     * Which {@code VideoConferenceProvider} owns the room, recorded per meeting
     * so meetings created under one provider keep working after the deployment
     * switches to another.
     */
    @Column(name = "provider_name", length = 64)
    private String providerName;

    @Column(name = "provider_room_id", length = 255)
    private String providerRoomId;
}
