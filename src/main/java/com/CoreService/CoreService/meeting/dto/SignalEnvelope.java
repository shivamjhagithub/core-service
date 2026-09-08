package com.CoreService.CoreService.meeting.dto;

import com.CoreService.CoreService.meeting.enums.SignalType;

import java.time.Instant;
import java.util.UUID;

/**
 * What peers actually receive: the inbound frame with an authenticated sender
 * attached by the server.
 */
public record SignalEnvelope(UUID meetingId,
                             SignalType type,
                             String senderUserId,
                             String targetUserId,
                             String payload,
                             Instant sentAt) {
}
