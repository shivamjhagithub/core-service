package com.CoreService.CoreService.material.dto;

import com.CoreService.CoreService.material.enums.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * The classroom of a material is immutable: moving it would silently change who
 * is allowed to see it.
 */
public record MaterialUpdateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Material type is required")
        MaterialType materialType,

        UUID fileId,

        @Size(max = 2000, message = "Link must not exceed 2000 characters")
        @Pattern(regexp = "^https?://.+", message = "Link must be an http or https URL")
        String linkUrl,

        UUID subjectId,

        UUID topicId) {
}
