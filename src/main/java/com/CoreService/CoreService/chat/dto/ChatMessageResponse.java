package com.CoreService.CoreService.chat.dto;

import com.CoreService.CoreService.chat.enums.MessageType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The single representation of a message: returned by REST history and pushed
 * verbatim over the room topic, so live and replayed messages look identical to
 * a client.
 */
public record ChatMessageResponse(UUID messageId,
                                  UUID chatRoomId,
                                  String senderUserId,
                                  String content,
                                  MessageType messageType,
                                  UUID fileId,
                                  boolean deleted,
                                  LocalDateTime createdAt) {
}
