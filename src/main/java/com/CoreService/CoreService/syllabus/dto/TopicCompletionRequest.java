package com.CoreService.CoreService.syllabus.dto;

import jakarta.validation.constraints.NotNull;

public record TopicCompletionRequest(
        @NotNull(message = "Completion flag is required")
        Boolean completed) {
}
