package com.CoreService.CoreService.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Creates the teacher record for an existing user of the caller's college. The
 * college is never accepted from the client; it comes from the request context.
 */
public record TeacherRequest(

        @NotBlank(message = "User id is required")
        @Size(max = 64, message = "User id must not exceed 64 characters")
        String userId,

        @Size(max = 64, message = "Employee id must not exceed 64 characters")
        String employeeId,

        @Size(max = 100, message = "Designation must not exceed 100 characters")
        String designation,

        @Size(max = 255, message = "Qualification must not exceed 255 characters")
        String qualification,

        UUID departmentId) {
}
