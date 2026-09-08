package com.CoreService.CoreService.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record DashboardMeetingItem(UUID meetingId,
                                   UUID classroomId,
                                   String title,
                                   Instant scheduledStartTime) {
}
