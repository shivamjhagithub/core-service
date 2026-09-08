package com.CoreService.CoreService.assignment.dto;

import com.CoreService.CoreService.assignment.enums.AssignmentStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @param submissionCount only populated for classroom teachers
 * @param mySubmission    the calling student's own submission, if any
 */
public record AssignmentDetailResponse(UUID id,
                                       UUID classroomId,
                                       UUID subjectId,
                                       String subjectName,
                                       String title,
                                       String description,
                                       String createdBy,
                                       Instant dueAt,
                                       Double maxMarks,
                                       AssignmentStatus status,
                                       Instant publishedAt,
                                       List<AttachmentResponse> attachments,
                                       Long submissionCount,
                                       SubmissionResponse mySubmission,
                                       LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
}
