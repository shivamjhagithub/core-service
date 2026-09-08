package com.CoreService.CoreService.meeting.dto;

import com.CoreService.CoreService.meeting.enums.ParticipantRole;

import java.time.Instant;

public record MeetingParticipantResponse(String userId,
                                         ParticipantRole role,
                                         Instant joinedAt,
                                         Instant leftAt) {
}
