package com.CoreService.CoreService.file.dto;

import java.util.UUID;

/**
 * Handle to a stored file. Business modules keep only this reference so the
 * storage backend can change without touching them.
 */
public record StoredFileRef(UUID fileId,
                            String originalFileName,
                            String contentType,
                            long fileSize) {
}
