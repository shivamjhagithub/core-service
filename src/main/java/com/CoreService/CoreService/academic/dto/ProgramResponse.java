package com.CoreService.CoreService.academic.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String name,
        String code,
        Integer durationYears,
        String description,
        UUID departmentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
