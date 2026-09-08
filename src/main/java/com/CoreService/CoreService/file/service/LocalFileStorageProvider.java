package com.CoreService.CoreService.file.service;

import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Filesystem backend, suitable for single-node deployments and development.
 * Keys are stored relative to the configured base directory so the volume can
 * be remounted elsewhere without rewriting rows.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageProvider implements FileStorageProvider {

    private static final String PROVIDER_NAME = "local";

    private final Path baseDirectory;

    public LocalFileStorageProvider(@Value("${app.storage.local.base-path}") String basePath) {
        this.baseDirectory = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String write(byte[] content, String key) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write file to local storage", e);
        }
        return key;
    }

    @Override
    public Resource read(String storagePath) {
        Path source = resolve(storagePath);
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new ResourceNotFoundException("File content is no longer available");
        }
        return new FileSystemResource(source);
    }

    @Override
    public void remove(String storagePath) {
        try {
            Files.deleteIfExists(resolve(storagePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete file from local storage", e);
        }
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    /**
     * Containment check: a stored path must never escape the base directory,
     * however it was built.
     */
    private Path resolve(String key) {
        Path resolved = baseDirectory.resolve(key).normalize();
        if (!resolved.startsWith(baseDirectory) || resolved.equals(baseDirectory)) {
            throw new BusinessException("Invalid storage location");
        }
        return resolved;
    }
}
