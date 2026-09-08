package com.CoreService.CoreService.attendance.entity;

import com.CoreService.CoreService.attendance.enums.AttendanceStatus;
import com.CoreService.CoreService.common.entity.TenantAwareEntity;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_records_session_student",
                columnNames = {"attendance_session_id", "student_user_id"}),
        indexes = {
                @Index(name = "idx_attendance_records_college_id", columnList = "college_id"),
                @Index(name = "idx_attendance_records_session_id", columnList = "attendance_session_id"),
                @Index(name = "idx_attendance_records_student_user_id", columnList = "student_user_id")
        })
public class AttendanceRecord extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_session_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_attendance_records_session"))
    private AttendanceSession session;

    @Column(name = "student_user_id", nullable = false, updatable = false, length = 64)
    private String studentUserId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "remark", length = 500)
    private String remark;
}
