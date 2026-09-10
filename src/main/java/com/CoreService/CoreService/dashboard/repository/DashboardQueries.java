package com.CoreService.CoreService.dashboard.repository;

import com.CoreService.CoreService.dashboard.dto.CollegeAdminDashboardResponse.AttendanceStatistics;
import com.CoreService.CoreService.dashboard.dto.CollegeAdminDashboardResponse.Totals;
import com.CoreService.CoreService.dashboard.dto.DashboardAnnouncementItem;
import com.CoreService.CoreService.dashboard.dto.DashboardMeetingItem;
import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse.AttendanceSummary;
import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse.RecentMaterial;
import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse.SubjectAttendance;
import com.CoreService.CoreService.dashboard.dto.StudentDashboardResponse.UpcomingAssignment;
import com.CoreService.CoreService.dashboard.dto.TeacherDashboardResponse.ClassroomStudentAttendance;
import com.CoreService.CoreService.dashboard.dto.TeacherDashboardResponse.RecentActivity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every cross-module read the dashboards need, as narrow native projections.
 * <p>
 * The dashboard owns no tables and maps no entities of its own: mapping another
 * module's table here would quietly create a second owner of that schema. Read
 * only aggregates in one auditable place is the trade-off instead, and every
 * statement is bound to a single {@code college_id} parameter, never a
 * concatenated one.
 */
@Repository
public class DashboardQueries {

    private static final String UPCOMING_ASSIGNMENTS_SQL = """
            SELECT a.id, a.classroom_id, a.title, a.due_at
              FROM assignments a
             WHERE a.college_id = :collegeId
               AND a.classroom_id IN (:classroomIds)
               AND a.status = 'PUBLISHED'
               AND a.due_at >= :now
             ORDER BY a.due_at ASC
            """;

    private static final String RECENT_ANNOUNCEMENTS_SQL = """
            SELECT an.id, an.title, an.target, an.published_at
              FROM announcements an
             WHERE an.college_id = :collegeId
               AND an.published = true
             ORDER BY an.published_at DESC NULLS LAST
            """;

    private static final String STUDENT_ATTENDANCE_BY_SUBJECT_SQL = """
            SELECT ses.subject_id,
                   sub.name AS subject_name,
                   COALESCE(SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present_count,
                   COALESCE(SUM(CASE WHEN ar.status = 'LATE' THEN 1 ELSE 0 END), 0) AS late_count,
                   COALESCE(SUM(CASE WHEN ar.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent_count,
                   COALESCE(SUM(CASE WHEN ar.status = 'EXCUSED' THEN 1 ELSE 0 END), 0) AS excused_count
              FROM attendance_records ar
              JOIN attendance_sessions ses ON ses.id = ar.attendance_session_id
                                          AND ses.college_id = ar.college_id
              LEFT JOIN academic_subjects sub ON sub.id = ses.subject_id
                                             AND sub.college_id = ar.college_id
             WHERE ar.college_id = :collegeId
               AND ar.student_user_id = :studentUserId
             GROUP BY ses.subject_id, sub.name
             ORDER BY sub.name IS NULL, sub.name ASC
            """;

    private static final String CLASSROOM_STUDENT_ATTENDANCE_SQL = """
            SELECT cm.classroom_id,
                   cm.user_id,
                   u.user_name,
                   COALESCE(SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present_count,
                   COALESCE(SUM(CASE WHEN ar.status = 'LATE' THEN 1 ELSE 0 END), 0) AS late_count,
                   COALESCE(SUM(CASE WHEN ar.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent_count,
                   COALESCE(SUM(CASE WHEN ar.status = 'EXCUSED' THEN 1 ELSE 0 END), 0) AS excused_count
              FROM classroom_members cm
              LEFT JOIN users u ON u.user_id = cm.user_id
              LEFT JOIN attendance_sessions ses ON ses.college_id = cm.college_id
                                               AND ses.classroom_id = cm.classroom_id
              LEFT JOIN attendance_records ar ON ar.college_id = cm.college_id
                                             AND ar.attendance_session_id = ses.id
                                             AND ar.student_user_id = cm.user_id
             WHERE cm.college_id = :collegeId
               AND cm.classroom_id IN (:classroomIds)
               AND cm.member_type = 'STUDENT'
             GROUP BY cm.classroom_id, cm.user_id, u.user_name
             ORDER BY u.user_name ASC
            """;

    private static final String UPCOMING_MEETINGS_SQL = """
            SELECT m.id, m.classroom_id, m.title, m.scheduled_start_time
              FROM meetings m
             WHERE m.college_id = :collegeId
               AND m.classroom_id IN (:classroomIds)
               AND m.scheduled_start_time >= :now
             ORDER BY m.scheduled_start_time ASC
            """;

    private static final String RECENT_MATERIALS_SQL = """
            SELECT sm.id, sm.classroom_id, sm.title, sm.created_at
              FROM study_materials sm
             WHERE sm.college_id = :collegeId
               AND sm.classroom_id IN (:classroomIds)
             ORDER BY sm.created_at DESC
            """;

    private static final String STUDENT_COUNTS_SQL = """
            SELECT cm.classroom_id, COUNT(*) AS student_count
              FROM classroom_members cm
             WHERE cm.college_id = :collegeId
               AND cm.classroom_id IN (:classroomIds)
               AND cm.member_type = 'STUDENT'
             GROUP BY cm.classroom_id
            """;

    private static final String AWAITING_GRADING_SQL = """
            SELECT COUNT(*)
              FROM assignment_submissions s
              JOIN assignments a ON a.id = s.assignment_id AND a.college_id = s.college_id
             WHERE s.college_id = :collegeId
               AND a.classroom_id IN (:classroomIds)
               AND s.status <> 'GRADED'
            """;

    /** Each branch binds its own copy of the tenant filter and the classroom list. */
    private static final String RECENT_ACTIVITY_SQL = """
            SELECT activity.activity_type, activity.classroom_id, activity.title, activity.occurred_at
              FROM (
                    SELECT 'ASSIGNMENT_PUBLISHED' AS activity_type,
                           a.classroom_id AS classroom_id,
                           a.title AS title,
                           a.created_at AS occurred_at
                      FROM assignments a
                     WHERE a.college_id = :collegeIdAssignments
                       AND a.classroom_id IN (:classroomIdsAssignments)
                    UNION ALL
                    SELECT 'MATERIAL_UPLOADED', sm.classroom_id, sm.title, sm.created_at
                      FROM study_materials sm
                     WHERE sm.college_id = :collegeIdMaterials
                       AND sm.classroom_id IN (:classroomIdsMaterials)
                    UNION ALL
                    SELECT 'SUBMISSION_RECEIVED', sa.classroom_id, sa.title, s.created_at
                      FROM assignment_submissions s
                      JOIN assignments sa ON sa.id = s.assignment_id AND sa.college_id = s.college_id
                     WHERE s.college_id = :collegeIdSubmissions
                       AND sa.classroom_id IN (:classroomIdsSubmissions)
                   ) activity
             ORDER BY activity.occurred_at DESC
            """;

    private static final String COLLEGE_TOTALS_SQL = """
            SELECT (SELECT COUNT(*) FROM students st
                     WHERE st.college_id = :collegeId AND st.active = true) AS total_students,
                   (SELECT COUNT(*) FROM teachers t
                     WHERE t.college_id = :collegeId AND t.active = true) AS total_teachers,
                   (SELECT COUNT(*) FROM classrooms c
                     WHERE c.college_id = :collegeId AND c.active = true AND c.archived = false) AS active_classrooms
            """;

    private static final String COLLEGE_ATTENDANCE_SQL = """
            SELECT COALESCE(SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present_count,
                   COUNT(*) AS total_count
              FROM attendance_records ar
              JOIN attendance_sessions ses ON ses.id = ar.attendance_session_id
                                          AND ses.college_id = ar.college_id
             WHERE ar.college_id = :collegeId
               AND ses.session_date >= :fromDate
            """;

    private static final String ENABLED_MODULES_SQL = """
            SELECT sm.module_code
              FROM college_modules cm
              JOIN system_modules sm ON sm.module_id = cm.module_id
             WHERE cm.college_id = :collegeId
               AND cm.enabled = true
             ORDER BY sm.module_code
            """;

    private final EntityManager entityManager;

    public DashboardQueries(EntityManager entityManager) {
        super();
        this.entityManager = entityManager;
    }

    public List<UpcomingAssignment> upcomingAssignments(UUID collegeId,
                                                        Collection<UUID> classroomIds,
                                                        Instant now,
                                                        int limit) {

        Query query = entityManager.createNativeQuery(UPCOMING_ASSIGNMENTS_SQL)
                .setParameter("collegeId", collegeId)
                .setParameter("classroomIds", classroomIds)
                .setParameter("now", now)
                .setMaxResults(limit);

        return rows(query).stream()
                .map(row -> new UpcomingAssignment(toUuid(row[0]), toUuid(row[1]), toText(row[2]), toInstant(row[3])))
                .toList();
    }

    public List<DashboardAnnouncementItem> recentAnnouncements(UUID collegeId, int limit) {
        Query query = entityManager.createNativeQuery(RECENT_ANNOUNCEMENTS_SQL)
                .setParameter("collegeId", collegeId)
                .setMaxResults(limit);

        return rows(query).stream()
                .map(row -> new DashboardAnnouncementItem(
                        toUuid(row[0]), toText(row[1]), toText(row[2]), toInstant(row[3])))
                .toList();
    }

    public AttendanceSummary attendanceOfStudent(UUID collegeId, String studentUserId) {
        Query query = entityManager.createNativeQuery(STUDENT_ATTENDANCE_BY_SUBJECT_SQL)
                .setParameter("collegeId", collegeId)
                .setParameter("studentUserId", studentUserId);

        List<SubjectAttendance> bySubject = rows(query).stream()
                .map(row -> SubjectAttendance.of(
                        toUuid(row[0]),
                        toText(row[1]),
                        toLong(row[2]),
                        toLong(row[3]),
                        toLong(row[4]),
                        toLong(row[5])))
                .toList();

        if (bySubject.isEmpty()) {
            return AttendanceSummary.empty();
        }

        return AttendanceSummary.of(
                bySubject.stream().mapToLong(SubjectAttendance::presentCount).sum(),
                bySubject.stream().mapToLong(SubjectAttendance::lateCount).sum(),
                bySubject.stream().mapToLong(SubjectAttendance::absentCount).sum(),
                bySubject.stream().mapToLong(SubjectAttendance::excusedCount).sum(),
                bySubject);
    }

    public Map<UUID, List<ClassroomStudentAttendance>> studentAttendanceByClassroom(
            UUID collegeId, Collection<UUID> classroomIds) {

        Query query = entityManager.createNativeQuery(CLASSROOM_STUDENT_ATTENDANCE_SQL)
                .setParameter("collegeId", collegeId)
                .setParameter("classroomIds", classroomIds);

        Map<UUID, List<ClassroomStudentAttendance>> byClassroom = new HashMap<>();
        for (Object[] row : rows(query)) {
            UUID classroomId = toUuid(row[0]);
            if (classroomId == null) {
                continue;
            }
            long present = toLong(row[3]);
            long late = toLong(row[4]);
            long absent = toLong(row[5]);
            long excused = toLong(row[6]);
            long total = present + late + absent + excused;
            byClassroom.computeIfAbsent(classroomId, key -> new ArrayList<>())
                    .add(new ClassroomStudentAttendance(
                            toText(row[1]),
                            toText(row[2]),
                            present,
                            late,
                            absent,
                            excused,
                            total,
                            AttendanceSummary.attendedPercentage(present, late, total)));
        }
        return byClassroom;
    }

    public List<DashboardMeetingItem> upcomingMeetings(UUID collegeId,
                                                       Collection<UUID> classroomIds,
                                                       Instant now,
                                                       int limit) {

        Query query = entityManager.createNativeQuery(UPCOMING_MEETINGS_SQL)
                .setParameter("collegeId", collegeId)
                .setParameter("classroomIds", classroomIds)
                .setParameter("now", now)
                .setMaxResults(limit);

        return rows(query).stream()
                .map(row -> new DashboardMeetingItem(
                        toUuid(row[0]), toUuid(row[1]), toText(row[2]), toInstant(row[3])))
                .toList();
    }

    public List<RecentMaterial> recentMaterials(UUID collegeId, Collection<UUID> classroomIds, int limit) {
        Query query = entityManager.createNativeQuery(RECENT_MATERIALS_SQL)
                .setParameter("collegeId", collegeId)
                .setParameter("classroomIds", classroomIds)
                .setMaxResults(limit);

        return rows(query).stream()
                .map(row -> new RecentMaterial(toUuid(row[0]), toUuid(row[1]), toText(row[2]), toInstant(row[3])))
                .toList();
    }

    public Map<UUID, Long> studentCountsByClassroom(UUID collegeId, Collection<UUID> classroomIds) {
        Query query = entityManager.createNativeQuery(STUDENT_COUNTS_SQL)
                .setParameter("collegeId", collegeId)
                .setParameter("classroomIds", classroomIds);

        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows(query)) {
            counts.put(toUuid(row[0]), toLong(row[1]));
        }
        return counts;
    }

    public long submissionsAwaitingGrading(UUID collegeId, Collection<UUID> classroomIds) {
        Query query = entityManager.createNativeQuery(AWAITING_GRADING_SQL)
                .setParameter("collegeId", collegeId)
                .setParameter("classroomIds", classroomIds);

        return toLong(singleRow(query)[0]);
    }

    public List<RecentActivity> recentActivity(UUID collegeId, Collection<UUID> classroomIds, int limit) {
        Query query = entityManager.createNativeQuery(RECENT_ACTIVITY_SQL)
                .setParameter("collegeIdAssignments", collegeId)
                .setParameter("classroomIdsAssignments", classroomIds)
                .setParameter("collegeIdMaterials", collegeId)
                .setParameter("classroomIdsMaterials", classroomIds)
                .setParameter("collegeIdSubmissions", collegeId)
                .setParameter("classroomIdsSubmissions", classroomIds)
                .setMaxResults(limit);

        return rows(query).stream()
                .map(row -> new RecentActivity(toText(row[0]), toUuid(row[1]), toText(row[2]), toInstant(row[3])))
                .toList();
    }

    public Totals collegeTotals(UUID collegeId) {
        Query query = entityManager.createNativeQuery(COLLEGE_TOTALS_SQL)
                .setParameter("collegeId", collegeId);

        Object[] row = singleRow(query);
        return new Totals(toLong(row[0]), toLong(row[1]), toLong(row[2]));
    }

    public AttendanceStatistics attendanceStatistics(UUID collegeId, LocalDate from, int windowDays) {
        Query query = entityManager.createNativeQuery(COLLEGE_ATTENDANCE_SQL)
                .setParameter("collegeId", collegeId)
                .setParameter("fromDate", from);

        Object[] row = singleRow(query);
        return AttendanceStatistics.of(toLong(row[0]), toLong(row[1]), windowDays);
    }

    public List<String> enabledModuleCodes(UUID collegeId) {
        Query query = entityManager.createNativeQuery(ENABLED_MODULES_SQL)
                .setParameter("collegeId", collegeId);

        return rows(query).stream()
                .map(row -> toText(row[0]))
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    private List<Object[]> rows(Query query) {
        List<?> results = query.getResultList();
        List<Object[]> rows = new ArrayList<>(results.size());
        for (Object result : results) {
            rows.add(result instanceof Object[] row ? row : new Object[]{result});
        }
        return rows;
    }

    /** Aggregates without a GROUP BY always produce exactly one row. */
    private Object[] singleRow(Query query) {
        List<Object[]> rows = rows(query);
        return rows.isEmpty() ? new Object[]{0L, 0L, 0L} : rows.get(0);
    }

    private static UUID toUuid(Object value) {
        return switch (value) {
            case null -> null;
            case UUID uuid -> uuid;
            default -> parseUuid(value.toString());
        };
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String toText(Object value) {
        return value == null ? null : value.toString();
    }

    private static Instant toInstant(Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> instant;
            case Timestamp timestamp -> timestamp.toInstant();
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
            case LocalDateTime localDateTime -> localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            default -> null;
        };
    }
}
