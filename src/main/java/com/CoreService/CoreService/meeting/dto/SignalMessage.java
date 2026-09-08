package com.CoreService.CoreService.meeting.dto;

import com.CoreService.CoreService.meeting.enums.SignalType;

import java.util.UUID;

/**
 * Body of {@code /app/meeting/signal}. There is no sender field by design: the
 * server stamps the sender from the session principal, so a peer cannot forge
 * an offer that appears to come from somebody else.
 * <p>
 * {@code payload} is the opaque WebRTC SDP or ICE candidate blob. The server
 * does not interpret it.
 */
public record SignalMessage(UUID meetingId,
                            SignalType type,
                            String targetUserId,
                            String payload) {
}
