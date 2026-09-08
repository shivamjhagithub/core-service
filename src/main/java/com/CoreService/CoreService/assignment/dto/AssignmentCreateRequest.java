package com.CoreService.CoreService.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentCreateRequest(
        @NotNull(message = "Classroom is required")
        UUID classroomId,

        UUID subjectId,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 4000, message = "Description must not exceed 4000 characters")
        String description,

        Instant dueAt,

        @PositiveOrZero(message = "Maximum marks must not be negative")
        Double maxMarks,

        @Size(max = 20, message = "An assignment cannot carry more than 20 attachments")
        List<UUID> attachmentFileIds) {
}
