package com.CoreService.CoreService.material.dto;

import com.CoreService.CoreService.material.enums.MaterialType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File details are resolved through the file module and stay {@code null} for
 * link materials.
 */
public record MaterialResponse(UUID id,
                               String title,
                               String description,
                               MaterialType materialType,
                               UUID fileId,
                               String fileName,
                               String contentType,
                               Long fileSize,
                               String linkUrl,
                               UUID classroomId,
                               UUID subjectId,
                               UUID topicId,
                               String uploadedBy,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
}
