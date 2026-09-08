package com.CoreService.CoreService.assignment.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AssignmentPublishedEvent(UUID collegeId,
                                       UUID assignmentId,
                                       UUID classroomId,
                                       String title,
                                       Instant dueAt,
                                       Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "assignment-published:" + assignmentId;
    }
}
