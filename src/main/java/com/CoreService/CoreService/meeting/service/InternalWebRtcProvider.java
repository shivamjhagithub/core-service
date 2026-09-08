package com.CoreService.CoreService.meeting.service;

import com.CoreService.CoreService.meeting.enums.ParticipantRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Default provider: peers exchange media directly over WebRTC and this
 * application only relays signaling over STOMP. There is nothing to allocate or
 * release on a third party, so the room identifier is derived from the meeting
 * itself and stays stable for the meeting's whole life.
 */
@Component
@ConditionalOnProperty(name = "app.meeting.provider", havingValue = "internal-webrtc", matchIfMissing = true)
public class InternalWebRtcProvider implements VideoConferenceProvider {

    public static final String PROVIDER_NAME = "internal-webrtc";

    private static final String ROOM_PREFIX = "webrtc-";

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public String createRoom(UUID meetingId, String title) {
        return ROOM_PREFIX + meetingId;
    }

    /**
     * A correlation handle, not a credential: with the internal provider every
     * signaling frame is authorized on arrival against the session's JWT and
     * the meeting roster, so nothing is granted by holding this value.
     */
    @Override
    public String joinToken(UUID meetingId, String userId, ParticipantRole role) {
        String handle = meetingId + "|" + userId + "|" + role;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(handle.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void closeRoom(String providerRoomId) {
        // Nothing is reserved outside this application, so ending the meeting is
        // the only cleanup: the signaling relay stops accepting frames for it.
    }
}
