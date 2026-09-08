package com.CoreService.CoreService.dashboard.dto;

import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse.AttendanceSummary;

import java.util.List;

public record CollegeAdminDashboardResponse(long totalStudents,
                                            long totalTeachers,
                                            long activeClassrooms,
                                            AttendanceStatistics attendance,
                                            List<String> enabledModuleCodes) {

    /** Head-count totals, read in one query. */
    public record Totals(long totalStudents, long totalTeachers, long activeClassrooms) {
    }

    public record AttendanceStatistics(long presentCount,
                                       long totalCount,
                                       double attendancePercentage,
                                       int windowDays) {

        public static AttendanceStatistics of(long presentCount, long totalCount, int windowDays) {
            return new AttendanceStatistics(presentCount, totalCount,
                    AttendanceSummary.percentage(presentCount, totalCount), windowDays);
        }
    }
}
