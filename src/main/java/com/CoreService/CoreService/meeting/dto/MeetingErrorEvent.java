package com.CoreService.CoreService.meeting.dto;

import java.time.Instant;

/**
 * Sent back to the originating session when a signaling frame is rejected, so a
 * peer can fall back instead of waiting for an answer that will never arrive.
 */
public record MeetingErrorEvent(String destination, String reason, Instant occurredAt) {
}
