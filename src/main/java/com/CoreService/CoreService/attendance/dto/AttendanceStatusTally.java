package com.CoreService.CoreService.attendance.dto;

import com.CoreService.CoreService.attendance.enums.AttendanceStatus;

/**
 * Grouped count of one status for one student, so a classroom report is one
 * aggregate query instead of a query per student.
 */
public record AttendanceStatusTally(String studentUserId, AttendanceStatus status, Long count) {
}
