package com.CoreService.CoreService.chat.dto;

import java.time.Instant;

/**
 * Sent back to the originating session when a chat frame is rejected. Without
 * it a refused message would simply vanish, since STOMP sends carry no reply.
 */
public record ChatErrorEvent(String destination, String reason, Instant occurredAt) {
}
