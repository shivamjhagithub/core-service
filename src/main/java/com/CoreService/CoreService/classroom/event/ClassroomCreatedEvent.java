package com.CoreService.CoreService.classroom.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ClassroomCreatedEvent(UUID collegeId,
                                    UUID classroomId,
                                    String classroomName,
                                    String createdBy,
                                    Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "classroom-created:" + classroomId;
    }
}
