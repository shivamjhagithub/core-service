package com.CoreService.CoreService.file.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ownership record for a blob held by the active {@code FileStorageProvider}.
 * The bytes live outside the database; this row is what makes them belong to a
 * college.
 */
@Entity
@Table(name = "stored_files", indexes = {
        @Index(name = "idx_stored_files_college_id", columnList = "college_id"),
        @Index(name = "idx_stored_files_uploaded_by", columnList = "uploaded_by")
})
@Getter
@Setter
@NoArgsConstructor
public class StoredFile extends TenantAwareEntity {

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /** Provider-relative key, never an absolute path, so the backend can move. */
    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "uploaded_by", nullable = false, length = 64)
    private String uploadedBy;

    @Column(name = "storage_provider", nullable = false, length = 32)
    private String storageProvider;
}
