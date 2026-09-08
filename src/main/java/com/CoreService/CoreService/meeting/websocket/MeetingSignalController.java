package com.CoreService.CoreService.meeting.websocket;

import com.CoreService.CoreService.common.exception.ApiException;
import com.CoreService.CoreService.common.security.Permissions;
import com.CoreService.CoreService.common.websocket.StompPrincipal;
import com.CoreService.CoreService.common.websocket.WebSocketAccessDeniedException;
import com.CoreService.CoreService.common.websocket.WebSocketDestinations;
import com.CoreService.CoreService.meeting.dto.MeetingErrorEvent;
import com.CoreService.CoreService.meeting.dto.SignalEnvelope;
import com.CoreService.CoreService.meeting.dto.SignalMessage;
import com.CoreService.CoreService.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

/**
 * WebRTC signaling relay. The server forwards offers, answers and ICE
 * candidates between peers and never sees media: the {@code payload} field is
 * opaque and is passed through untouched.
 * <p>
 * Every frame is authorized on its own. A session that was an active
 * participant a moment ago may have been removed, or the meeting may have
 * ended, so nothing is cached between frames.
 */
@Controller
@RequiredArgsConstructor
public class MeetingSignalController {

    private static final Logger log = LoggerFactory.getLogger(MeetingSignalController.class);

    private final MeetingService meetingService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping(MeetingDestinations.SIGNAL_MAPPING)
    public void signal(@Payload SignalMessage message, StompPrincipal principal) {
        if (!principal.hasPermission(Permissions.MEETING_JOIN)) {
            throw new WebSocketAccessDeniedException("Missing permission " + Permissions.MEETING_JOIN);
        }
        if (message == null || message.meetingId() == null || message.type() == null) {
            throw new WebSocketAccessDeniedException("meetingId and type are required");
        }

        UUID meetingId = message.meetingId();
        UUID collegeId = principal.getCollegeId();
        String senderUserId = principal.getUserId();

        if (collegeId == null || !meetingService.isActiveParticipant(meetingId, collegeId, senderUserId)) {
            throw new WebSocketAccessDeniedException("You are not an active participant of this meeting");
        }

        // The sender is stamped from the session, so the relayed frame always
        // states who really sent it whatever the payload claimed.
        SignalEnvelope envelope = new SignalEnvelope(
                meetingId,
                message.type(),
                senderUserId,
                message.targetUserId(),
                message.payload(),
                Instant.now());

        switch (message.type()) {
            case OFFER, ANSWER, ICE_CANDIDATE -> relayToPeer(envelope, collegeId);
            case JOIN, LEAVE -> messagingTemplate.convertAndSend(
                    WebSocketDestinations.meetingTopic(meetingId), envelope);
        }
    }

    /**
     * Only deliberate refusals describe themselves; anything unexpected is
     * logged here and reported generically rather than handing internals to a
     * client.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public MeetingErrorEvent handleFailure(Exception exception) {
        boolean expected = exception instanceof WebSocketAccessDeniedException
                || exception instanceof ApiException;

        if (expected) {
            log.debug("Rejected meeting signal frame: {}", exception.getMessage());
        } else {
            log.warn("Failed to handle meeting signal frame", exception);
        }

        return new MeetingErrorEvent(MeetingDestinations.SIGNAL_DESTINATION,
                expected ? exception.getMessage() : "Signal could not be processed",
                Instant.now());
    }

    /**
     * Peer-to-peer frames are checked against the sender's own college, which is
     * what prevents signaling from crossing a tenant boundary.
     */
    private void relayToPeer(SignalEnvelope envelope, UUID collegeId) {
        String targetUserId = envelope.targetUserId();
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new WebSocketAccessDeniedException(envelope.type() + " requires a targetUserId");
        }
        if (!meetingService.isActiveParticipant(envelope.meetingId(), collegeId, targetUserId)) {
            throw new WebSocketAccessDeniedException("The target is not an active participant of this meeting");
        }
        messagingTemplate.convertAndSendToUser(
                targetUserId, WebSocketDestinations.USER_SIGNAL_QUEUE, envelope);
    }
}
