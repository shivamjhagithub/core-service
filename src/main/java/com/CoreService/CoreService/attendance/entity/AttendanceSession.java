package com.CoreService.CoreService.attendance.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "attendance_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_sessions_classroom_date_subject",
                columnNames = {"college_id", "classroom_id", "session_date", "subject_id"}),
        indexes = {
                @Index(name = "idx_attendance_sessions_college_id", columnList = "college_id"),
                @Index(name = "idx_attendance_sessions_classroom_id", columnList = "classroom_id"),
                @Index(name = "idx_attendance_sessions_session_date", columnList = "session_date")
        })
public class AttendanceSession extends TenantAwareEntity {

    @Column(name = "classroom_id", nullable = false, updatable = false)
    private UUID classroomId;

    /** Optional: a session may cover the classroom as a whole rather than one subject. */
    @Column(name = "subject_id", updatable = false)
    private UUID subjectId;

    @Column(name = "teacher_user_id", nullable = false, updatable = false, length = 64)
    private String teacherUserId;

    @Column(name = "session_date", nullable = false, updatable = false)
    private LocalDate sessionDate;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "locked", nullable = false)
    private boolean locked;
}
