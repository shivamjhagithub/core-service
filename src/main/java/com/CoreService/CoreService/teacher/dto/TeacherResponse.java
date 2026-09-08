package com.CoreService.CoreService.teacher.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TeacherResponse(
        UUID id,
        String userId,
        String employeeId,
        String designation,
        String qualification,
        UUID departmentId,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
