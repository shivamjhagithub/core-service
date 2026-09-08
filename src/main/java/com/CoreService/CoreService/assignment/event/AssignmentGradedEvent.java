package com.CoreService.CoreService.assignment.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AssignmentGradedEvent(UUID collegeId,
                                    UUID assignmentId,
                                    UUID submissionId,
                                    String studentUserId,
                                    String title,
                                    Double marks,
                                    Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "assignment-graded:" + submissionId;
    }
}
