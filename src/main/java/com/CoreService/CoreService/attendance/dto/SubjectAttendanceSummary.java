package com.CoreService.CoreService.attendance.dto;

import java.util.UUID;

/** Subject id and name are null for sessions that were not tied to a subject. */
public record SubjectAttendanceSummary(UUID subjectId,
                                       String subjectName,
                                       AttendanceTotals totals) {
}
