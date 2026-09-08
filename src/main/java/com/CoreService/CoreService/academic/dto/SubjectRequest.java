package com.CoreService.CoreService.academic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubjectRequest(

        @NotBlank(message = "Subject name is required")
        @Size(max = 150, message = "Subject name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Subject code is required")
        @Size(max = 32, message = "Subject code must not exceed 32 characters")
        String code,

        @NotNull(message = "Credits are required")
        @Positive(message = "Credits must be a positive number")
        @Max(value = 20, message = "Credits must not exceed 20")
        Integer credits,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Semester is required")
        UUID semesterId) {
}
