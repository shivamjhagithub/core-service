package com.CoreService.CoreService.meeting.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * The host is taken from the authenticated context, so it is not part of the
 * request. {@code classroomId} is optional: without it the meeting is ad-hoc
 * and only its host and the people it records as participants can reach it.
 */
public record ScheduleMeetingRequest(UUID classroomId,
                                     @NotBlank @Size(max = 255) String title,
                                     @Size(max = 2000) String description,
                                     @NotNull @Future Instant scheduledStartTime,
                                     @Future Instant scheduledEndTime) {
}
