package com.CoreService.CoreService.chat.dto;

import java.util.UUID;

/** Body of {@code /app/chat/join}. */
public record ChatJoinFrame(UUID chatRoomId) {
}
