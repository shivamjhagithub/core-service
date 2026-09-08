package com.CoreService.CoreService.meeting.dto;

import com.CoreService.CoreService.meeting.enums.MeetingStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Broadcast on the meeting topic when a meeting changes state, so clients that
 * are waiting in a lobby learn about it without polling.
 */
public record MeetingStatusEvent(UUID meetingId,
                                 MeetingStatus status,
                                 Instant occurredAt) {
}
