package com.CoreService.CoreService.chat.dto;

import com.CoreService.CoreService.chat.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * REST counterpart of the STOMP send frame. It carries no sender: the service
 * is handed the identity from the authenticated context instead.
 */
public record SendMessageRequest(@NotNull UUID chatRoomId,
                                 @Size(max = 4000) String content,
                                 @NotNull MessageType messageType,
                                 UUID fileId) {
}
