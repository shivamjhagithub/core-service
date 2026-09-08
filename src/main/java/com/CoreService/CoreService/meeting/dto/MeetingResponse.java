package com.CoreService.CoreService.meeting.dto;

import com.CoreService.CoreService.meeting.enums.MeetingStatus;

import java.time.Instant;
import java.util.UUID;

public record MeetingResponse(UUID meetingId,
                              UUID classroomId,
                              String title,
                              String description,
                              String hostUserId,
                              Instant scheduledStartTime,
                              Instant scheduledEndTime,
                              Instant actualStartTime,
                              Instant actualEndTime,
                              MeetingStatus status,
                              String providerName) {
}
