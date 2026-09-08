package com.CoreService.CoreService.chat.dto;

import com.CoreService.CoreService.chat.enums.ChatPermissionMode;
import com.CoreService.CoreService.chat.enums.ChatRoomType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChatRoomResponse(UUID chatRoomId,
                               String name,
                               ChatRoomType roomType,
                               UUID classroomId,
                               ChatPermissionMode permissionMode,
                               List<String> participantUserIds,
                               LocalDateTime createdAt) {
}
