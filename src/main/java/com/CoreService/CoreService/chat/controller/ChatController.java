package com.CoreService.CoreService.chat.controller;

import com.CoreService.CoreService.chat.dto.ChatMessageResponse;
import com.CoreService.CoreService.chat.dto.ChatRoomResponse;
import com.CoreService.CoreService.chat.dto.ChatUnreadCountResponse;
import com.CoreService.CoreService.chat.dto.OpenDirectRoomRequest;
import com.CoreService.CoreService.chat.dto.SendMessageRequest;
import com.CoreService.CoreService.chat.dto.UpdatePermissionModeRequest;
import com.CoreService.CoreService.chat.service.ChatService;
import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.TenantGuard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * CHAT_VIEW covers taking part in a conversation passively (resolving a room,
 * reading history, read state); CHAT_SEND covers changing it. Which rooms and
 * messages a holder of either permission may actually touch is decided in the
 * service from participation and classroom role.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final TenantGuard tenantGuard;

    @PostMapping("/direct-rooms")
    @PreAuthorize("hasAuthority('CHAT_VIEW')")
    public ApiResponse<ChatRoomResponse> openDirectRoom(@Valid @RequestBody OpenDirectRoomRequest request) {
        return ApiResponse.success("Direct chat room ready",
                chatService.openDirectRoom(request.otherUserId()));
    }

    @PostMapping("/classroom-rooms/{classroomId}")
    @PreAuthorize("hasAuthority('CHAT_VIEW')")
    public ApiResponse<ChatRoomResponse> classroomRoom(@PathVariable UUID classroomId) {
        return ApiResponse.success("Classroom chat room ready",
                chatService.getOrCreateClassroomRoom(classroomId));
    }

    @GetMapping("/rooms/{chatRoomId}/messages")
    @PreAuthorize("hasAuthority('CHAT_VIEW')")
    public ApiResponse<PageResponse<ChatMessageResponse>> history(
            @PathVariable UUID chatRoomId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return ApiResponse.success(chatService.history(chatRoomId, page, size));
    }

    @PostMapping("/rooms/{chatRoomId}/read")
    @PreAuthorize("hasAuthority('CHAT_VIEW')")
    public ApiResponse<Void> markAsRead(@PathVariable UUID chatRoomId) {
        chatService.markAsRead(chatRoomId);
        return ApiResponse.message("Chat room marked as read");
    }

    @GetMapping("/unread-counts")
    @PreAuthorize("hasAuthority('CHAT_VIEW')")
    public ApiResponse<List<ChatUnreadCountResponse>> unreadCounts() {
        return ApiResponse.success(chatService.unreadCounts());
    }

    /**
     * Fallback for clients without a live STOMP session; it enters the same
     * service method as {@code /app/chat/send} and broadcasts identically.
     */
    @PostMapping("/messages")
    @PreAuthorize("hasAuthority('CHAT_SEND')")
    public ApiResponse<ChatMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        ChatMessageResponse message = chatService.sendMessage(
                request.chatRoomId(),
                tenantGuard.requireUserId(),
                tenantGuard.requireCollegeId(),
                request.content(),
                request.messageType(),
                request.fileId());
        return ApiResponse.success("Message sent", message);
    }

    @PatchMapping("/rooms/{chatRoomId}/permission-mode")
    @PreAuthorize("hasAuthority('CHAT_SEND')")
    public ApiResponse<ChatRoomResponse> updatePermissionMode(
            @PathVariable UUID chatRoomId,
            @Valid @RequestBody UpdatePermissionModeRequest request) {
        return ApiResponse.success("Chat permission mode updated",
                chatService.updatePermissionMode(chatRoomId, request.permissionMode()));
    }

    @DeleteMapping("/messages/{messageId}")
    @PreAuthorize("hasAuthority('CHAT_SEND')")
    public ApiResponse<Void> deleteMessage(@PathVariable UUID messageId) {
        chatService.deleteMessage(messageId);
        return ApiResponse.message("Message deleted");
    }
}
