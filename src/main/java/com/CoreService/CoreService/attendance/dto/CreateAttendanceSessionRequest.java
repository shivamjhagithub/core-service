package com.CoreService.CoreService.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAttendanceSessionRequest(

        @NotNull(message = "Classroom id is required")
        UUID classroomId,

        // Optional: omit for a session that is not tied to a single subject.
        UUID subjectId,

        @NotNull(message = "Session date is required")
        LocalDate sessionDate,

        @Size(max = 500, message = "Remark must not exceed 500 characters")
        String remark) {
}
