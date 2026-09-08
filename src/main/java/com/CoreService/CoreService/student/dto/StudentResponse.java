package com.CoreService.CoreService.student.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String userId,
        String rollNumber,
        String registrationNumber,
        String fatherName,
        String bloodGroup,
        Integer semester,
        UUID departmentId,
        UUID programId,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
