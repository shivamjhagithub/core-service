package com.CoreService.CoreService.attendance.dto;

public record StudentAttendanceReportRow(String studentUserId,
                                         String studentName,
                                         AttendanceTotals totals) {
}
