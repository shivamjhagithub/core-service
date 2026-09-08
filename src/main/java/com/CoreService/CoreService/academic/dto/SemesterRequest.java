package com.CoreService.CoreService.academic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SemesterRequest(

        @NotNull(message = "Semester number is required")
        @Min(value = 1, message = "Semester number must be at least 1")
        @Max(value = 12, message = "Semester number must not exceed 12")
        Integer number,

        @NotBlank(message = "Semester name is required")
        @Size(max = 100, message = "Semester name must not exceed 100 characters")
        String name,

        @NotNull(message = "Program is required")
        UUID programId,

        UUID academicSessionId) {
}
