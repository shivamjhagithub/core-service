package com.CoreService.CoreService.file.controller;

import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.file.dto.StoredFileRef;
import com.CoreService.CoreService.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoredFileRef>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "category", defaultValue = "general") String category) {

        StoredFileRef storedFile = fileStorageService.store(file, category);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded", storedFile));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable UUID fileId) {
        StoredFileRef metadata = fileStorageService.describe(fileId);
        Resource content = fileStorageService.load(fileId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(metadata.originalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaTypeOf(metadata.contentType()))
                .contentLength(metadata.fileSize())
                .body(content);
    }

    @GetMapping("/{fileId}/metadata")
    public ApiResponse<StoredFileRef> metadata(@PathVariable UUID fileId) {
        return ApiResponse.success(fileStorageService.describe(fileId));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable UUID fileId) {
        fileStorageService.delete(fileId);
        return ApiResponse.message("File deleted");
    }

    private MediaType mediaTypeOf(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
