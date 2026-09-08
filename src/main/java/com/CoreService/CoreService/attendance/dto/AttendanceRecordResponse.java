package com.CoreService.CoreService.attendance.dto;

import com.CoreService.CoreService.attendance.enums.AttendanceStatus;

import java.util.UUID;

public record AttendanceRecordResponse(UUID id,
                                       String studentUserId,
                                       String studentName,
                                       AttendanceStatus status,
                                       String remark) {
}
