package com.CoreService.CoreService.meeting.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MeetingScheduledEvent(UUID collegeId,
                                    UUID meetingId,
                                    UUID classroomId,
                                    String title,
                                    Instant scheduledStartTime,
                                    Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "meeting-scheduled:" + meetingId;
    }
}
