package com.CoreService.CoreService.chat.mapper;

import com.CoreService.CoreService.chat.dto.ChatMessageResponse;
import com.CoreService.CoreService.chat.dto.ChatRoomResponse;
import com.CoreService.CoreService.chat.entity.ChatMessage;
import com.CoreService.CoreService.chat.entity.ChatRoom;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMapper {

    public ChatRoomResponse toRoomResponse(ChatRoom room, List<String> participantUserIds) {
        return new ChatRoomResponse(
                room.getId(),
                room.getName(),
                room.getRoomType(),
                room.getClassroomId(),
                room.getPermissionMode(),
                participantUserIds,
                room.getCreatedAt());
    }

    public ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderUserId(),
                message.getContent(),
                message.getMessageType(),
                message.getFileId(),
                message.isDeleted(),
                message.getCreatedAt());
    }
}
