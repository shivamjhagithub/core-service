package com.CoreService.CoreService.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MarkAttendanceRequest(

        @Valid
        @NotEmpty(message = "At least one attendance entry is required")
        @Size(max = 500, message = "At most 500 entries can be submitted at once")
        List<AttendanceEntry> entries) {
}
