package com.CoreService.CoreService.attendance.dto;

import com.CoreService.CoreService.attendance.enums.AttendanceStatus;

import java.util.UUID;

/** Grouped count of one status for one session. */
public record SessionStatusTally(UUID sessionId, AttendanceStatus status, Long count) {
}
