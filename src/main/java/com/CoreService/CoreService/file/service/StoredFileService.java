package com.CoreService.CoreService.file.service;

import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.file.dto.StoredFileRef;
import com.CoreService.CoreService.file.entity.StoredFile;
import com.CoreService.CoreService.file.mapper.StoredFileMapper;
import com.CoreService.CoreService.file.repository.StoredFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tenant-aware façade over the active {@link FileStorageProvider}. The
 * ownership row and the bytes are written in one transaction, and every read
 * path resolves a file by id <em>and</em> college so a college can never reach
 * another college's content.
 */
@Service
@PreAuthorize("isAuthenticated()")
public class StoredFileService implements FileStorageService, FileLookupService {

    private static final Logger log = LoggerFactory.getLogger(StoredFileService.class);

    private static final String RESOURCE_TYPE = "File";
    private static final String DEFAULT_CATEGORY = "general";
    private static final String FALLBACK_FILE_NAME = "file";
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final Pattern CATEGORY_DISALLOWED = Pattern.compile("[^a-zA-Z0-9_-]");
    private static final Pattern FILE_NAME_DISALLOWED = Pattern.compile("[\\p{Cntrl}\"*:<>?|]");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[A-Za-z0-9]{1,12}");

    private final StoredFileRepository storedFileRepository;
    private final FileStorageProvider storageProvider;
    private final StoredFileMapper storedFileMapper;
    private final TenantGuard tenantGuard;
    private final long maxFileSizeBytes;

    public StoredFileService(StoredFileRepository storedFileRepository,
                             FileStorageProvider storageProvider,
                             StoredFileMapper storedFileMapper,
                             TenantGuard tenantGuard,
                             @Value("${app.storage.max-file-size-bytes}") long maxFileSizeBytes) {
        this.storedFileRepository = storedFileRepository;
        this.storageProvider = storageProvider;
        this.storedFileMapper = storedFileMapper;
        this.tenantGuard = tenantGuard;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional
    public StoredFileRef store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file is empty");
        }
        assertWithinSizeLimit(file.getSize());

        UUID collegeId = tenantGuard.requireCollegeId();
        String uploadedBy = tenantGuard.requireUserId();
        String fileName = sanitizeFileName(file.getOriginalFilename());

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }
        assertWithinSizeLimit(content.length);

        String key = buildKey(collegeId, category, fileName);
        String storagePath = storageProvider.write(content, key);

        StoredFile storedFile = new StoredFile();
        storedFile.setCollegeId(collegeId);
        storedFile.setOriginalFileName(fileName);
        storedFile.setContentType(resolveContentType(file.getContentType()));
        storedFile.setFileSize(content.length);
        storedFile.setStoragePath(storagePath);
        storedFile.setUploadedBy(uploadedBy);
        storedFile.setStorageProvider(storageProvider.name());

        return storedFileMapper.toRef(storedFileRepository.save(storedFile));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource load(UUID fileId) {
        return storageProvider.read(requireOwnedFile(fileId).getStoragePath());
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFileRef describe(UUID fileId) {
        return storedFileMapper.toRef(requireOwnedFile(fileId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileRef> describeAll(Collection<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> distinctIds = fileIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        return storedFileRepository.findByIdInAndCollegeId(distinctIds, tenantGuard.requireCollegeId())
                .stream()
                .map(storedFileMapper::toRef)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID fileId) {
        StoredFile storedFile = requireOwnedFile(fileId);
        storedFileRepository.delete(storedFile);
        storageProvider.remove(storedFile.getStoragePath());
    }

    private StoredFile requireOwnedFile(UUID fileId) {
        if (fileId == null) {
            throw new ResourceNotFoundException(RESOURCE_TYPE + " not found");
        }
        StoredFile storedFile = storedFileRepository
                .findByIdAndCollegeId(fileId, tenantGuard.requireCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, fileId));

        if (!storageProvider.name().equals(storedFile.getStorageProvider())) {
            // Reading a foreign provider's path through the active backend would
            // resolve to an unrelated location, so refuse instead of guessing.
            log.warn("File {} was written by storage provider '{}' but '{}' is active",
                    fileId, storedFile.getStorageProvider(), storageProvider.name());
            throw new BusinessException("File is not available from the active storage provider");
        }
        return storedFile;
    }

    private void assertWithinSizeLimit(long size) {
        if (size > maxFileSizeBytes) {
            throw new BusinessException("File exceeds the maximum allowed size of " + maxFileSizeBytes + " bytes");
        }
    }

    private String buildKey(UUID collegeId, String category, String fileName) {
        return collegeId + "/" + sanitizeCategory(category) + "/" + UUID.randomUUID() + extensionOf(fileName);
    }

    private String sanitizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return DEFAULT_CATEGORY;
        }
        String cleaned = CATEGORY_DISALLOWED.matcher(category.trim()).replaceAll("");
        return cleaned.isEmpty() ? DEFAULT_CATEGORY : cleaned.toLowerCase(Locale.ROOT);
    }

    /**
     * Keeps only the base name: a client-supplied name may contain directory
     * separators and is echoed back in the download header.
     */
    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null) {
            return FALLBACK_FILE_NAME;
        }
        String name = originalFileName.replace('\\', '/');
        int lastSeparator = name.lastIndexOf('/');
        if (lastSeparator >= 0) {
            name = name.substring(lastSeparator + 1);
        }
        name = FILE_NAME_DISALLOWED.matcher(name).replaceAll("").trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            return FALLBACK_FILE_NAME;
        }
        return name.length() > MAX_FILE_NAME_LENGTH
                ? name.substring(name.length() - MAX_FILE_NAME_LENGTH)
                : name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(dot + 1);
        return SAFE_EXTENSION.matcher(extension).matches()
                ? "." + extension.toLowerCase(Locale.ROOT)
                : "";
    }

    private String resolveContentType(String contentType) {
        return contentType == null || contentType.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : contentType;
    }
}
