package com.CoreService.CoreService.file.service;

import com.CoreService.CoreService.file.dto.StoredFileRef;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * File module's published API. Implementations may store bytes locally, in
 * MinIO or in S3; business modules never learn which.
 */
public interface FileStorageService {

    /**
     * Persists the upload and records ownership against the caller's college.
     *
     * @param category logical folder, e.g. {@code "materials"} or {@code "submissions"}
     */
    StoredFileRef store(MultipartFile file, String category);

    /** Loads file content, rejecting files belonging to another college. */
    Resource load(UUID fileId);

    StoredFileRef describe(UUID fileId);

    void delete(UUID fileId);
}
