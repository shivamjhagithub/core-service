package com.CoreService.CoreService.academic.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubjectResponse(
        UUID id,
        String name,
        String code,
        Integer credits,
        String description,
        UUID semesterId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
