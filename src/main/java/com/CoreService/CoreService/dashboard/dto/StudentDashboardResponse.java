package com.CoreService.CoreService.dashboard.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentDashboardResponse(List<UpcomingAssignment> upcomingAssignments,
                                       List<DashboardAnnouncementItem> recentAnnouncements,
                                       AttendanceSummary attendance,
                                       List<DashboardMeetingItem> upcomingMeetings,
                                       List<RecentMaterial> recentMaterials) {

    public record UpcomingAssignment(UUID assignmentId, UUID classroomId, String title, Instant dueAt) {
    }

    public record RecentMaterial(UUID materialId, UUID classroomId, String title, Instant uploadedAt) {
    }

    public record AttendanceSummary(long presentCount, long totalCount, double attendancePercentage) {

        public static AttendanceSummary of(long presentCount, long totalCount) {
            return new AttendanceSummary(presentCount, totalCount, percentage(presentCount, totalCount));
        }

        static double percentage(long presentCount, long totalCount) {
            if (totalCount <= 0) {
                return 0d;
            }
            return BigDecimal.valueOf(presentCount * 100d / totalCount)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }
}
