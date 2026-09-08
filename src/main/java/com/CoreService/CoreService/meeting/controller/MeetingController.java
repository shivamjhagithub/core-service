package com.CoreService.CoreService.meeting.controller;

import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.meeting.dto.MeetingDetailResponse;
import com.CoreService.CoreService.meeting.dto.MeetingJoinResponse;
import com.CoreService.CoreService.meeting.dto.MeetingResponse;
import com.CoreService.CoreService.meeting.dto.ScheduleMeetingRequest;
import com.CoreService.CoreService.meeting.dto.UpdateMeetingRequest;
import com.CoreService.CoreService.meeting.service.MeetingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Lifecycle transitions are separate endpoints rather than a status field on the
 * update payload, so each one carries its own rules and cannot be reached by
 * writing a status a client should not be able to set.
 */
@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    @PreAuthorize("hasAuthority('MEETING_CREATE')")
    public ApiResponse<MeetingResponse> schedule(@Valid @RequestBody ScheduleMeetingRequest request) {
        return ApiResponse.success("Meeting scheduled", meetingService.schedule(request));
    }

    @PutMapping("/{meetingId}")
    @PreAuthorize("hasAuthority('MEETING_UPDATE')")
    public ApiResponse<MeetingResponse> update(@PathVariable UUID meetingId,
                                               @Valid @RequestBody UpdateMeetingRequest request) {
        return ApiResponse.success("Meeting updated", meetingService.update(meetingId, request));
    }

    @PostMapping("/{meetingId}/cancel")
    @PreAuthorize("hasAuthority('MEETING_UPDATE')")
    public ApiResponse<MeetingResponse> cancel(@PathVariable UUID meetingId) {
        return ApiResponse.success("Meeting cancelled", meetingService.cancel(meetingId));
    }

    @PostMapping("/{meetingId}/start")
    @PreAuthorize("hasAuthority('MEETING_UPDATE')")
    public ApiResponse<MeetingResponse> start(@PathVariable UUID meetingId) {
        return ApiResponse.success("Meeting started", meetingService.start(meetingId));
    }

    @PostMapping("/{meetingId}/end")
    @PreAuthorize("hasAuthority('MEETING_UPDATE')")
    public ApiResponse<MeetingResponse> end(@PathVariable UUID meetingId) {
        return ApiResponse.success("Meeting ended", meetingService.end(meetingId));
    }

    @PostMapping("/{meetingId}/join")
    @PreAuthorize("hasAuthority('MEETING_JOIN')")
    public ApiResponse<MeetingJoinResponse> join(@PathVariable UUID meetingId) {
        return ApiResponse.success("Joined meeting", meetingService.join(meetingId));
    }

    @PostMapping("/{meetingId}/leave")
    @PreAuthorize("hasAuthority('MEETING_JOIN')")
    public ApiResponse<Void> leave(@PathVariable UUID meetingId) {
        meetingService.leave(meetingId);
        return ApiResponse.message("Left meeting");
    }

    @GetMapping("/{meetingId}")
    @PreAuthorize("hasAuthority('MEETING_VIEW')")
    public ApiResponse<MeetingDetailResponse> detail(@PathVariable UUID meetingId) {
        return ApiResponse.success(meetingService.detail(meetingId));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAuthority('MEETING_VIEW')")
    public ApiResponse<List<MeetingResponse>> upcoming() {
        return ApiResponse.success(meetingService.upcoming());
    }

    @GetMapping("/classrooms/{classroomId}")
    @PreAuthorize("hasAuthority('MEETING_VIEW')")
    public ApiResponse<PageResponse<MeetingResponse>> byClassroom(
            @PathVariable UUID classroomId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(meetingService.byClassroom(classroomId, page, size));
    }
}
