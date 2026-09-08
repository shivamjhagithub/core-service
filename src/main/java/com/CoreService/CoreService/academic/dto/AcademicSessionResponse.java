package com.CoreService.CoreService.academic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AcademicSessionResponse(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean current,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
