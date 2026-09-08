package com.CoreService.CoreService.file.service;

import org.springframework.core.io.Resource;

/**
 * Byte-level storage backend. Exactly one implementation is active per
 * deployment, selected by {@code app.storage.provider}; adding MinIO or S3 means
 * adding an implementation, not touching business modules.
 * <p>
 * Keys handed to {@link #write(byte[], String)} are provider-relative and
 * already tenant-prefixed by the caller.
 */
public interface FileStorageProvider {

    /**
     * Persists {@code content} under {@code key} and returns the storage path to
     * record on the owning row. Implementations must not overwrite an existing
     * object.
     */
    String write(byte[] content, String key);

    Resource read(String storagePath);

    void remove(String storagePath);

    /** Stable identifier persisted on every row this provider wrote. */
    String name();
}
