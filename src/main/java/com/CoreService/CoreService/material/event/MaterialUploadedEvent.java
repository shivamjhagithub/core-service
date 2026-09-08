package com.CoreService.CoreService.material.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MaterialUploadedEvent(UUID collegeId,
                                    UUID materialId,
                                    UUID classroomId,
                                    String title,
                                    Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "material-uploaded:" + materialId;
    }
}
