package com.CoreService.CoreService.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * {@code current} is honoured on both create and update; the service demotes any
 * other session of the college so only one stays current.
 */
public record AcademicSessionRequest(

        @NotBlank(message = "Session name is required")
        @Size(max = 64, message = "Session name must not exceed 64 characters")
        String name,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        boolean current) {
}
