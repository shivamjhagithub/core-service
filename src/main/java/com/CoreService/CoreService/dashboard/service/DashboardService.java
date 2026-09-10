package com.CoreService.CoreService.dashboard.service;

import com.CoreService.CoreService.classroom.dto.ClassroomRef;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.dashboard.dto.CollegeAdminDashboardResponse;
import com.CoreService.CoreService.dashboard.dto.CollegeAdminDashboardResponse.Totals;
import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse;
import com.CoreService.CoreService.dashboard.dto.TeacherDashboardResponse;
import com.CoreService.CoreService.dashboard.dto.TeacherDashboardResponse.ClassroomStudentAttendance;
import com.CoreService.CoreService.dashboard.dto.TeacherDashboardResponse.ClassroomSummary;
import com.CoreService.CoreService.dashboard.repository.DashboardQueries;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private static final int FEED_LIMIT = 5;
    private static final int ACTIVITY_LIMIT = 10;
    private static final int ATTENDANCE_WINDOW_DAYS = 30;

    private final DashboardQueries dashboardQueries;
    private final ClassroomAccessService classroomAccessService;
    private final TenantGuard tenantGuard;

    @Transactional(readOnly = true)
    public StudentDashboardResponse studentDashboard() {
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();
        List<UUID> classroomIds = myClassroomIds(userId);
        Instant now = Instant.now();

        // Announcements and attendance are not classroom scoped, so they are still
        // worth reading for a student who has not been enrolled anywhere yet; the
        // classroom scoped queries are skipped because an empty IN () is not SQL.
        if (classroomIds.isEmpty()) {
            return new StudentDashboardResponse(
                    List.of(),
                    dashboardQueries.recentAnnouncements(collegeId, FEED_LIMIT),
                    dashboardQueries.attendanceOfStudent(collegeId, userId),
                    List.of(),
                    List.of());
        }

        return new StudentDashboardResponse(
                dashboardQueries.upcomingAssignments(collegeId, classroomIds, now, FEED_LIMIT),
                dashboardQueries.recentAnnouncements(collegeId, FEED_LIMIT),
                dashboardQueries.attendanceOfStudent(collegeId, userId),
                dashboardQueries.upcomingMeetings(collegeId, classroomIds, now, FEED_LIMIT),
                dashboardQueries.recentMaterials(collegeId, classroomIds, FEED_LIMIT));
    }

    @Transactional(readOnly = true)
    public TeacherDashboardResponse teacherDashboard() {
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();
        List<UUID> classroomIds = myClassroomIds(userId);

        if (classroomIds.isEmpty()) {
            return TeacherDashboardResponse.empty();
        }

        return new TeacherDashboardResponse(
                classroomSummaries(classroomIds,
                        dashboardQueries.studentAttendanceByClassroom(collegeId, classroomIds)),
                dashboardQueries.submissionsAwaitingGrading(collegeId, classroomIds),
                dashboardQueries.upcomingMeetings(collegeId, classroomIds, Instant.now(), FEED_LIMIT),
                dashboardQueries.recentActivity(collegeId, classroomIds, ACTIVITY_LIMIT));
    }

    @Transactional(readOnly = true)
    public CollegeAdminDashboardResponse collegeAdminDashboard() {
        UUID collegeId = tenantGuard.requireCollegeId();

        Totals totals = dashboardQueries.collegeTotals(collegeId);

        return new CollegeAdminDashboardResponse(
                totals.totalStudents(),
                totals.totalTeachers(),
                totals.activeClassrooms(),
                dashboardQueries.attendanceStatistics(collegeId,
                        LocalDate.now().minusDays(ATTENDANCE_WINDOW_DAYS), ATTENDANCE_WINDOW_DAYS),
                dashboardQueries.enabledModuleCodes(collegeId));
    }

    /**
     * Names come from the classroom module rather than from a join, because the
     * dashboard is not allowed to own a second reading of the classroom schema.
     */
    private List<ClassroomSummary> classroomSummaries(
            List<UUID> classroomIds,
            Map<UUID, List<ClassroomStudentAttendance>> attendanceByClassroom) {

        List<ClassroomSummary> summaries = new ArrayList<>(classroomIds.size());
        for (UUID classroomId : classroomIds) {
            try {
                ClassroomRef classroom = classroomAccessService.requireClassroom(classroomId);
                List<ClassroomStudentAttendance> students =
                        List.copyOf(attendanceByClassroom.getOrDefault(classroomId, List.of()));
                summaries.add(new ClassroomSummary(classroom.classroomId(), classroom.name(),
                        classroom.active(), students.size(), students));
            } catch (RuntimeException ex) {
                log.debug("Leaving classroom {} out of the dashboard: {}", classroomId, ex.toString());
            }
        }
        return List.copyOf(summaries);
    }

    private List<UUID> myClassroomIds(String userId) {
        List<UUID> classroomIds = classroomAccessService.classroomIdsOfUser(userId);
        return classroomIds == null ? List.of() : classroomIds;
    }
}
