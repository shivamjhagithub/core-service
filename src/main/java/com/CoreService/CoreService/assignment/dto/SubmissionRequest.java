package com.CoreService.CoreService.assignment.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SubmissionRequest(
        @Size(max = 4000, message = "Content must not exceed 4000 characters")
        String content,

        /** When {@code null} the existing attachments are kept as they are. */
        @Size(max = 20, message = "A submission cannot carry more than 20 attachments")
        List<UUID> attachmentFileIds) {
}
