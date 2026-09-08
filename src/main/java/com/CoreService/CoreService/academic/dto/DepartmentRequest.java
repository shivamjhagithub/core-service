package com.CoreService.CoreService.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(

        @NotBlank(message = "Department name is required")
        @Size(max = 150, message = "Department name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Department code is required")
        @Size(max = 32, message = "Department code must not exceed 32 characters")
        String code,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description) {
}
