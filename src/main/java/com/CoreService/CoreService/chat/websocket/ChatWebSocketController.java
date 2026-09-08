package com.CoreService.CoreService.chat.websocket;

import com.CoreService.CoreService.chat.dto.ChatErrorEvent;
import com.CoreService.CoreService.chat.dto.ChatJoinFrame;
import com.CoreService.CoreService.chat.dto.ChatSendFrame;
import com.CoreService.CoreService.chat.enums.MessageType;
import com.CoreService.CoreService.chat.service.ChatService;
import com.CoreService.CoreService.common.exception.ApiException;
import com.CoreService.CoreService.common.security.Permissions;
import com.CoreService.CoreService.common.websocket.StompPrincipal;
import com.CoreService.CoreService.common.websocket.WebSocketAccessDeniedException;
import com.CoreService.CoreService.common.websocket.WebSocketDestinations;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.time.Instant;

/**
 * STOMP entry points for chat.
 * <p>
 * Method security does not apply to STOMP frames, so the permission the REST
 * layer expresses with {@code @PreAuthorize} is asserted here against the
 * session principal. Identity fields are never read from the frame body: the
 * sender is always the authenticated principal of the session that sent it.
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);
    private static final String DESTINATION = WebSocketDestinations.APP_PREFIX + "/chat";

    private final ChatService chatService;

    /** Client destination: {@code /app/chat/send}. */
    @MessageMapping("/chat/send")
    public void send(@Payload ChatSendFrame frame, StompPrincipal principal) {
        requirePermission(principal, Permissions.CHAT_SEND);
        if (frame == null || frame.chatRoomId() == null) {
            throw new WebSocketAccessDeniedException("chatRoomId is required");
        }
        MessageType messageType = frame.messageType() == null ? MessageType.TEXT : frame.messageType();

        chatService.sendMessage(
                frame.chatRoomId(),
                principal.getUserId(),
                principal.getCollegeId(),
                frame.content(),
                messageType,
                frame.fileId());
    }

    /** Client destination: {@code /app/chat/join}. */
    @MessageMapping("/chat/join")
    public void join(@Payload ChatJoinFrame frame, StompPrincipal principal) {
        requirePermission(principal, Permissions.CHAT_VIEW);
        if (frame == null || frame.chatRoomId() == null) {
            throw new WebSocketAccessDeniedException("chatRoomId is required");
        }

        chatService.joinRoom(frame.chatRoomId(), principal.getUserId(), principal.getCollegeId());
    }

    /**
     * Reports the refusal to the sending session only, so one client's rejected
     * frame is never visible to the rest of the room. Only deliberate refusals
     * describe themselves; anything unexpected is logged here and reported
     * generically rather than handing internals to a client.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ChatErrorEvent handleFailure(Exception exception) {
        boolean expected = exception instanceof WebSocketAccessDeniedException
                || exception instanceof ApiException;

        if (expected) {
            log.debug("Rejected chat frame: {}", exception.getMessage());
        } else {
            log.warn("Failed to handle chat frame", exception);
        }

        return new ChatErrorEvent(DESTINATION,
                expected ? exception.getMessage() : "Message could not be processed",
                Instant.now());
    }

    private void requirePermission(StompPrincipal principal, String permission) {
        if (!principal.hasPermission(permission)) {
            throw new WebSocketAccessDeniedException("Missing permission " + permission);
        }
    }
}
