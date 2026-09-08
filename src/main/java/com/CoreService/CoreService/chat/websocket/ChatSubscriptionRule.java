package com.CoreService.CoreService.chat.websocket;

import com.CoreService.CoreService.chat.repository.ChatParticipantRepository;
import com.CoreService.CoreService.common.websocket.StompPrincipal;
import com.CoreService.CoreService.common.websocket.StompSubscriptionRule;
import com.CoreService.CoreService.common.websocket.WebSocketDestinationAuthorizer;
import com.CoreService.CoreService.common.websocket.WebSocketDestinations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Guards {@code /topic/chat/{chatRoomId}}: only a participant of that room, in
 * the college the session was authenticated for, may listen to it.
 * <p>
 * Two constraints shape this class. It runs while the SUBSCRIBE frame is still
 * being authorized, before any request context is bound, so the check must pass
 * the college explicitly instead of relying on {@code TenantGuard}. And it is
 * constructed as part of the inbound channel, so it depends on the repository
 * directly rather than on a service that would drag the messaging template into
 * the channel's own dependency graph.
 */
@Component
@RequiredArgsConstructor
public class ChatSubscriptionRule implements StompSubscriptionRule {

    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public boolean supports(String destination) {
        return destination != null
                && destination.startsWith(WebSocketDestinations.CHAT_ROOM_TOPIC_PREFIX);
    }

    @Override
    public boolean isAllowed(String destination, StompPrincipal principal) {
        UUID chatRoomId = WebSocketDestinationAuthorizer.parseTrailingUuid(
                destination, WebSocketDestinations.CHAT_ROOM_TOPIC_PREFIX);

        if (chatRoomId == null || principal.getCollegeId() == null) {
            return false;
        }
        return chatParticipantRepository.isParticipant(
                principal.getCollegeId(), chatRoomId, principal.getUserId());
    }
}
