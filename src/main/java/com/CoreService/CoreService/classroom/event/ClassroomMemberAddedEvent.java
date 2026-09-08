package com.CoreService.CoreService.classroom.event;

import com.CoreService.CoreService.classroom.enums.ClassroomMemberType;
import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClassroomMemberAddedEvent(UUID collegeId,
                                        UUID classroomId,
                                        String classroomName,
                                        List<String> memberUserIds,
                                        ClassroomMemberType memberType,
                                        Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "classroom-member-added:" + classroomId + ":" + occurredAt.toEpochMilli();
    }
}
