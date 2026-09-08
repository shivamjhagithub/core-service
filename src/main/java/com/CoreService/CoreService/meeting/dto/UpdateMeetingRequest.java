package com.CoreService.CoreService.meeting.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Replaces the schedulable fields of a meeting. The classroom a meeting belongs
 * to is fixed at creation, because it is what its access rules are built on.
 */
public record UpdateMeetingRequest(@NotBlank @Size(max = 255) String title,
                                   @Size(max = 2000) String description,
                                   @NotNull @Future Instant scheduledStartTime,
                                   @Future Instant scheduledEndTime) {
}
