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

    /**
     * Overall totals plus one row per subject. {@code subjectId} / {@code subjectName}
     * are null for sessions that were not tied to a subject. PRESENT and LATE
     * count as attended, matching the attendance module.
     */
    public record AttendanceSummary(long presentCount,
                                   long lateCount,
                                   long absentCount,
                                   long excusedCount,
                                   long totalCount,
                                   double attendancePercentage,
                                   List<SubjectAttendance> bySubject) {

        public static AttendanceSummary empty() {
            return of(0L, 0L, 0L, 0L, List.of());
        }

        public static AttendanceSummary of(long presentCount, long totalCount) {
            long safePresent = Math.max(presentCount, 0L);
            long safeTotal = Math.max(totalCount, 0L);
            long remainder = Math.max(safeTotal - safePresent, 0L);
            return of(safePresent, 0L, remainder, 0L, List.of());
        }

        public static AttendanceSummary of(long presentCount,
                                           long lateCount,
                                           long absentCount,
                                           long excusedCount,
                                           List<SubjectAttendance> bySubject) {

            long total = presentCount + lateCount + absentCount + excusedCount;
            return new AttendanceSummary(presentCount, lateCount, absentCount, excusedCount, total,
                    attendedPercentage(presentCount, lateCount, total),
                    bySubject == null ? List.of() : List.copyOf(bySubject));
        }

        static double percentage(long presentCount, long totalCount) {
            return attendedPercentage(presentCount, 0L, totalCount);
        }

        public static double attendedPercentage(long presentCount, long lateCount, long totalCount) {
            if (totalCount <= 0) {
                return 0d;
            }
            return BigDecimal.valueOf((presentCount + lateCount) * 100d / totalCount)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }

    public record SubjectAttendance(UUID subjectId,
                                    String subjectName,
                                    long presentCount,
                                    long lateCount,
                                    long absentCount,
                                    long excusedCount,
                                    long totalCount,
                                    double attendancePercentage) {

        public static SubjectAttendance of(UUID subjectId,
                                           String subjectName,
                                           long presentCount,
                                           long lateCount,
                                           long absentCount,
                                           long excusedCount) {

            long total = presentCount + lateCount + absentCount + excusedCount;
            return new SubjectAttendance(subjectId, subjectName, presentCount, lateCount, absentCount,
                    excusedCount, total, AttendanceSummary.attendedPercentage(presentCount, lateCount, total));
        }
    }
}
