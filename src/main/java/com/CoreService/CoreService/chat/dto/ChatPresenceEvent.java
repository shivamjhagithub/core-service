package com.CoreService.CoreService.chat.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Pushed to the room topic when a participant opens a room, so peers can render
 * presence and read state without polling.
 */
public record ChatPresenceEvent(UUID chatRoomId,
                                String userId,
                                Instant occurredAt) {
}
