package com.CoreService.CoreService.assignment.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AssignmentSubmittedEvent(UUID collegeId,
                                       UUID assignmentId,
                                       UUID submissionId,
                                       UUID classroomId,
                                       String studentUserId,
                                       boolean late,
                                       Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "assignment-submitted:" + submissionId;
    }
}
