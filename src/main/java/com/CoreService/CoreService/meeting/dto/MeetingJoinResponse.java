package com.CoreService.CoreService.meeting.dto;

import com.CoreService.CoreService.meeting.enums.ParticipantRole;

import java.util.UUID;

/**
 * Everything a client needs to enter a meeting: the provider handle plus the
 * STOMP destinations that carry signaling for it. The destinations are returned
 * rather than hard-coded in the client so switching to a hosted provider stays
 * a server-side change.
 */
public record MeetingJoinResponse(UUID meetingId,
                                  ParticipantRole role,
                                  String providerName,
                                  String providerRoomId,
                                  String joinToken,
                                  String signalSendDestination,
                                  String meetingTopic,
                                  String signalQueue) {
}
