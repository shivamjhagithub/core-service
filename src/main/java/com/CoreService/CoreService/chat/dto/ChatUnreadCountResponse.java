package com.CoreService.CoreService.chat.dto;

import java.util.UUID;

/**
 * Projection target of the unread-count aggregate. {@code unreadCount} is boxed
 * because JPQL {@code count()} binds a {@code Long} into the constructor.
 */
public record ChatUnreadCountResponse(UUID chatRoomId, Long unreadCount) {
}
