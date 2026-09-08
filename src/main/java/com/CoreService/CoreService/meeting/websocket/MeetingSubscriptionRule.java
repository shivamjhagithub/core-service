package com.CoreService.CoreService.meeting.websocket;

import com.CoreService.CoreService.common.websocket.StompPrincipal;
import com.CoreService.CoreService.common.websocket.StompSubscriptionRule;
import com.CoreService.CoreService.common.websocket.WebSocketDestinationAuthorizer;
import com.CoreService.CoreService.common.websocket.WebSocketDestinations;
import com.CoreService.CoreService.meeting.repository.MeetingParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Guards {@code /topic/meeting/{meetingId}}: only somebody on that meeting's
 * roster, in the college the session was authenticated for, may listen to it.
 * <p>
 * Membership rather than presence is required here, so a participant who has
 * left can resubscribe and rejoin. Relaying signaling is stricter and does
 * demand presence.
 * <p>
 * Like the chat rule, this runs before any request context is bound and is
 * built as part of the inbound channel, so it takes the college explicitly and
 * depends only on the repository.
 */
@Component
@RequiredArgsConstructor
public class MeetingSubscriptionRule implements StompSubscriptionRule {

    private final MeetingParticipantRepository meetingParticipantRepository;

    @Override
    public boolean supports(String destination) {
        return destination != null
                && destination.startsWith(WebSocketDestinations.MEETING_TOPIC_PREFIX);
    }

    @Override
    public boolean isAllowed(String destination, StompPrincipal principal) {
        UUID meetingId = WebSocketDestinationAuthorizer.parseTrailingUuid(
                destination, WebSocketDestinations.MEETING_TOPIC_PREFIX);

        if (meetingId == null || principal.getCollegeId() == null) {
            return false;
        }
        return meetingParticipantRepository.isParticipant(
                principal.getCollegeId(), meetingId, principal.getUserId());
    }
}
