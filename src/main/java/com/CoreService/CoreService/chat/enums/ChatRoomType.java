package com.CoreService.CoreService.chat.enums;

/**
 * Shape of a chat room. The type decides which authorization source applies:
 * DIRECT rooms are governed by participation alone, CLASSROOM rooms additionally
 * obey the classroom's own permission settings.
 */
public enum ChatRoomType {
    DIRECT,
    CLASSROOM
}
