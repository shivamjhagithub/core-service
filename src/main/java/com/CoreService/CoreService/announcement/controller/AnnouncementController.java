package com.CoreService.CoreService.announcement.controller;

import com.CoreService.CoreService.announcement.dto.AnnouncementRequest;
import com.CoreService.CoreService.announcement.dto.AnnouncementResponse;
import com.CoreService.CoreService.announcement.service.AnnouncementService;
import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_CREATE')")
    public ApiResponse<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.success("Announcement created", announcementService.create(request));
    }

    @PostMapping("/{announcementId}/publish")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_CREATE')")
    public ApiResponse<AnnouncementResponse> publish(@PathVariable UUID announcementId) {
        return ApiResponse.success("Announcement published", announcementService.publish(announcementId));
    }

    @DeleteMapping("/{announcementId}")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID announcementId) {
        announcementService.delete(announcementId);
        return ApiResponse.message("Announcement deleted");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_VIEW')")
    public ApiResponse<PageResponse<AnnouncementResponse>> visibleToMe(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "publishedAt"));

        return ApiResponse.success(announcementService.visibleToMe(pageable));
    }

    @GetMapping("/{announcementId}")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_VIEW')")
    public ApiResponse<AnnouncementResponse> getById(@PathVariable UUID announcementId) {
        return ApiResponse.success(announcementService.getById(announcementId));
    }
}
