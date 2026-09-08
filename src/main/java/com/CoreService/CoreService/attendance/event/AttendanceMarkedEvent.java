package com.CoreService.CoreService.attendance.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttendanceMarkedEvent(UUID collegeId,
                                    UUID attendanceSessionId,
                                    UUID classroomId,
                                    List<String> studentUserIds,
                                    Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "attendance-marked:" + attendanceSessionId;
    }
}
