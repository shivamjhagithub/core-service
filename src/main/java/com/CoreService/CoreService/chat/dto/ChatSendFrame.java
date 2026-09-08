package com.CoreService.CoreService.chat.dto;

import com.CoreService.CoreService.chat.enums.MessageType;

import java.util.UUID;

/**
 * Body of {@code /app/chat/send}. Deliberately has no sender or college field:
 * anything a client claims about its own identity is ignored, and the values
 * used come from the session's StompPrincipal.
 */
public record ChatSendFrame(UUID chatRoomId,
                            String content,
                            MessageType messageType,
                            UUID fileId) {
}
