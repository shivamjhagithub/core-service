package com.CoreService.CoreService.attendance.dto;

import com.CoreService.CoreService.attendance.enums.AttendanceStatus;

import java.util.UUID;

/** Grouped count of one status for one subject. A null subject id is allowed. */
public record SubjectStatusTally(UUID subjectId, AttendanceStatus status, Long count) {
}
