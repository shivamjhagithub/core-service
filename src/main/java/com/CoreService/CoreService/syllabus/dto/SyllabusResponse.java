package com.CoreService.CoreService.syllabus.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SyllabusResponse(UUID id,
                               String title,
                               String description,
                               UUID subjectId,
                               UUID classroomId,
                               boolean published,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
}
