package com.CoreService.CoreService.syllabus.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SyllabusTopicCompletedEvent(UUID collegeId,
                                          UUID syllabusId,
                                          UUID topicId,
                                          String topicTitle,
                                          Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "syllabus-topic-completed:" + topicId;
    }
}
