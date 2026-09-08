package com.CoreService.CoreService.attendance.dto;

import com.CoreService.CoreService.attendance.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAttendanceRecordRequest(

        @NotNull(message = "Attendance status is required")
        AttendanceStatus status,

        @Size(max = 500, message = "Remark must not exceed 500 characters")
        String remark) {
}
