package com.CoreService.CoreService.meeting.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MeetingStartedEvent(UUID collegeId,
                                  UUID meetingId,
                                  UUID classroomId,
                                  String title,
                                  Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "meeting-started:" + meetingId;
    }
}
