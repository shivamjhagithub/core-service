package com.CoreService.CoreService.teacher.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * {@code userId} is absent on purpose: the link between a teacher record and a
 * user is its identity and cannot be reassigned.
 */
public record TeacherUpdateRequest(

        @Size(max = 64, message = "Employee id must not exceed 64 characters")
        String employeeId,

        @Size(max = 100, message = "Designation must not exceed 100 characters")
        String designation,

        @Size(max = 255, message = "Qualification must not exceed 255 characters")
        String qualification,

        UUID departmentId) {
}
