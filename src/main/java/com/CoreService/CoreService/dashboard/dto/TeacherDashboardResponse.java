package com.CoreService.CoreService.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeacherDashboardResponse(List<ClassroomSummary> classrooms,
                                       long submissionsAwaitingGrading,
                                       List<DashboardMeetingItem> upcomingMeetings,
                                       List<RecentActivity> recentActivity) {

    public record ClassroomSummary(UUID classroomId, String name, boolean active, long studentCount) {
    }

    /**
     * @param activityType ASSIGNMENT_PUBLISHED, MATERIAL_UPLOADED or SUBMISSION_RECEIVED
     */
    public record RecentActivity(String activityType, UUID classroomId, String title, Instant occurredAt) {
    }

    public static TeacherDashboardResponse empty() {
        return new TeacherDashboardResponse(List.of(), 0L, List.of(), List.of());
    }
}
