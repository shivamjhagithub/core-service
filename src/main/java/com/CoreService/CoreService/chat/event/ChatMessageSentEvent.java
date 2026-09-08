package com.CoreService.CoreService.chat.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageSentEvent(UUID collegeId,
                                   UUID chatRoomId,
                                   UUID messageId,
                                   String senderUserId,
                                   String preview,
                                   List<String> recipientUserIds,
                                   Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "chat-message-sent:" + messageId;
    }
}
