package com.CoreService.CoreService.assignment.dto;

import com.CoreService.CoreService.assignment.enums.SubmissionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(UUID id,
                                 UUID assignmentId,
                                 String assignmentTitle,
                                 String studentUserId,
                                 String content,
                                 Instant submittedAt,
                                 SubmissionStatus status,
                                 Double marks,
                                 String feedback,
                                 String gradedBy,
                                 Instant gradedAt,
                                 List<AttachmentResponse> attachments) {
}
