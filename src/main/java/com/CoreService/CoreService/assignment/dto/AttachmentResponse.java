package com.CoreService.CoreService.assignment.dto;

import java.util.UUID;

/**
 * File metadata is resolved through the file module; the descriptive fields are
 * {@code null} when the referenced file no longer exists.
 */
public record AttachmentResponse(UUID fileId,
                                 String fileName,
                                 String contentType,
                                 Long fileSize) {
}
