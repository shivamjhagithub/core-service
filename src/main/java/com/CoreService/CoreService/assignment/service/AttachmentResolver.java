package com.CoreService.CoreService.assignment.service;

import com.CoreService.CoreService.assignment.dto.AttachmentResponse;
import com.CoreService.CoreService.assignment.dto.AttachmentRow;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.file.dto.StoredFileRef;
import com.CoreService.CoreService.file.service.FileLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns attachment references into presentable metadata with one file-module
 * lookup per request, whatever the number of assignments or submissions on the
 * page.
 */
@Service
@RequiredArgsConstructor
public class AttachmentResolver {

    private final FileLookupService fileLookupService;

    /**
     * Confirms every referenced file exists inside the caller's college and
     * returns its metadata, so a write can answer with attachment details
     * without a second lookup.
     */
    public List<StoredFileRef> requireOwnedFiles(List<UUID> fileIds) {
        List<UUID> distinctIds = fileIds == null
                ? List.of()
                : fileIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        List<StoredFileRef> refs = fileLookupService.describeAll(distinctIds);
        if (refs.size() != distinctIds.size()) {
            throw new ResourceNotFoundException("One or more attached files could not be found");
        }
        return refs;
    }

    public List<AttachmentResponse> toResponses(List<StoredFileRef> refs) {
        return refs.stream().map(this::toResponse).toList();
    }

    /**
     * Groups attachment rows by owner, filling in file metadata where the
     * referenced file still exists.
     */
    public Map<UUID, List<AttachmentResponse>> groupByOwner(List<AttachmentRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, StoredFileRef> refsById = fileLookupService
                .describeAll(rows.stream().map(AttachmentRow::fileId).toList())
                .stream()
                .collect(Collectors.toMap(StoredFileRef::fileId, Function.identity(), (first, second) -> first));

        Map<UUID, List<AttachmentResponse>> byOwner = new LinkedHashMap<>();
        for (AttachmentRow row : rows) {
            StoredFileRef ref = refsById.get(row.fileId());
            AttachmentResponse response = ref == null
                    ? new AttachmentResponse(row.fileId(), null, null, null)
                    : toResponse(ref);
            byOwner.computeIfAbsent(row.ownerId(), key -> new ArrayList<>()).add(response);
        }
        return byOwner;
    }

    private AttachmentResponse toResponse(StoredFileRef ref) {
        return new AttachmentResponse(ref.fileId(), ref.originalFileName(), ref.contentType(), ref.fileSize());
    }
}
