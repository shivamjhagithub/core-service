package com.CoreService.CoreService.academic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProgramRequest(

        @NotBlank(message = "Program name is required")
        @Size(max = 150, message = "Program name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Program code is required")
        @Size(max = 32, message = "Program code must not exceed 32 characters")
        String code,

        @NotNull(message = "Duration in years is required")
        @Positive(message = "Duration must be a positive number of years")
        @Max(value = 10, message = "Duration must not exceed 10 years")
        Integer durationYears,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Department is required")
        UUID departmentId) {
}
