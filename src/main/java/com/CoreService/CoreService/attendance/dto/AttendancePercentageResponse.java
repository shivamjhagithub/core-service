package com.CoreService.CoreService.attendance.dto;

import java.util.UUID;

public record AttendancePercentageResponse(UUID classroomId,
                                           String studentUserId,
                                           AttendanceTotals totals) {
}
