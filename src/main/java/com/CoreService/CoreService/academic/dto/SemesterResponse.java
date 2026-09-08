package com.CoreService.CoreService.academic.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SemesterResponse(
        UUID id,
        Integer number,
        String name,
        UUID programId,
        UUID academicSessionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
