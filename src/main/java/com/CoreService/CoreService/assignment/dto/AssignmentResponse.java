package com.CoreService.CoreService.assignment.dto;

import com.CoreService.CoreService.assignment.enums.AssignmentStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssignmentResponse(UUID id,
                                 UUID classroomId,
                                 UUID subjectId,
                                 String title,
                                 String description,
                                 String createdBy,
                                 Instant dueAt,
                                 Double maxMarks,
                                 AssignmentStatus status,
                                 Instant publishedAt,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
}
