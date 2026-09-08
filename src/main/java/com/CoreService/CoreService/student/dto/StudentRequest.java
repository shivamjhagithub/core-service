package com.CoreService.CoreService.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Creates the student record for an existing user of the caller's college. The
 * college is never accepted from the client; it comes from the request context.
 */
public record StudentRequest(

        @NotBlank(message = "User id is required")
        @Size(max = 64, message = "User id must not exceed 64 characters")
        String userId,

        @Size(max = 64, message = "Roll number must not exceed 64 characters")
        String rollNumber,

        @Size(max = 64, message = "Registration number must not exceed 64 characters")
        String registrationNumber,

        @Size(max = 150, message = "Father name must not exceed 150 characters")
        String fatherName,

        @Size(max = 8, message = "Blood group must not exceed 8 characters")
        String bloodGroup,

        @Min(value = 1, message = "Semester must be at least 1")
        @Max(value = 12, message = "Semester must not exceed 12")
        Integer semester,

        UUID departmentId,

        UUID programId) {
}
