package com.CoreService.CoreService.file.service;

import com.CoreService.CoreService.file.dto.StoredFileRef;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Batch companion to {@link FileStorageService}: modules that render lists of
 * attachments resolve every reference in one query instead of one per row.
 */
public interface FileLookupService {

    /**
     * Metadata for the given ids that exist inside the caller's college.
     * Unknown or foreign ids are silently omitted, so the result may be smaller
     * than the request.
     */
    List<StoredFileRef> describeAll(Collection<UUID> fileIds);
}
