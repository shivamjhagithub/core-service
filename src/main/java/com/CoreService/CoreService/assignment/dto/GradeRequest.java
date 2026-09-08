package com.CoreService.CoreService.assignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record GradeRequest(
        @NotNull(message = "Marks are required")
        @PositiveOrZero(message = "Marks must not be negative")
        Double marks,

        @Size(max = 2000, message = "Feedback must not exceed 2000 characters")
        String feedback) {
}
