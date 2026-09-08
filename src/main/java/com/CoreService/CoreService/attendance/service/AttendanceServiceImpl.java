package com.CoreService.CoreService.attendance.service;

import com.CoreService.CoreService.academic.service.AcademicLookupService;
import com.CoreService.CoreService.attendance.dto.AttendanceEntry;
import com.CoreService.CoreService.attendance.dto.AttendanceHistoryItem;
import com.CoreService.CoreService.attendance.dto.AttendancePercentageResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceRecordResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceSessionDetailResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceSessionResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceStatusTally;
import com.CoreService.CoreService.attendance.dto.AttendanceTotals;
import com.CoreService.CoreService.attendance.dto.ClassroomAttendanceReport;
import com.CoreService.CoreService.attendance.dto.CreateAttendanceSessionRequest;
import com.CoreService.CoreService.attendance.dto.MarkAttendanceRequest;
import com.CoreService.CoreService.attendance.dto.MyAttendanceResponse;
import com.CoreService.CoreService.attendance.dto.SessionStatusTally;
import com.CoreService.CoreService.attendance.dto.StudentAttendanceReportRow;
import com.CoreService.CoreService.attendance.dto.SubjectAttendanceSummary;
import com.CoreService.CoreService.attendance.dto.SubjectStatusTally;
import com.CoreService.CoreService.attendance.dto.UpdateAttendanceRecordRequest;
import com.CoreService.CoreService.attendance.entity.AttendanceRecord;
import com.CoreService.CoreService.attendance.entity.AttendanceSession;
import com.CoreService.CoreService.attendance.enums.AttendanceStatus;
import com.CoreService.CoreService.attendance.event.AttendanceMarkedEvent;
import com.CoreService.CoreService.attendance.mapper.AttendanceMapper;
import com.CoreService.CoreService.attendance.repository.AttendanceRecordRepository;
import com.CoreService.CoreService.attendance.repository.AttendanceSessionRepository;
import com.CoreService.CoreService.audit.service.AuditRecorder;
import com.CoreService.CoreService.classroom.dto.ClassroomRef;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.DuplicateResourceException;
import com.CoreService.CoreService.common.exception.ForbiddenException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.Permissions;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final String SESSION = "Attendance session";
    private static final String RECORD = "Attendance record";
    private static final String LOCKED_MESSAGE = "Attendance session is locked and can no longer be changed";

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceMapper attendanceMapper;
    private final ClassroomAccessService classroomAccessService;
    private final AcademicLookupService academicLookupService;
    private final UserRepo userRepo;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher eventPublisher;

    // ------------------------------------------------------------------
    // Sessions
    // ------------------------------------------------------------------

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    public AttendanceSessionDetailResponse createSession(CreateAttendanceSessionRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();
        String teacherUserId = tenantGuard.requireUserId();

        ClassroomRef classroom = classroomAccessService.requireClassroom(request.classroomId());
        classroomAccessService.assertTeacher(classroom.classroomId());

        if (!classroom.active()) {
            throw new BusinessException("Classroom is not active");
        }
        if (request.sessionDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Attendance cannot be taken for a future date");
        }
        if (request.subjectId() != null) {
            academicLookupService.requireSubject(request.subjectId());
        }
        assertSessionSlotFree(collegeId, classroom.classroomId(), request.sessionDate(), request.subjectId());

        AttendanceSession session = new AttendanceSession();
        session.setCollegeId(collegeId);
        session.setClassroomId(classroom.classroomId());
        session.setSubjectId(request.subjectId());
        session.setTeacherUserId(teacherUserId);
        session.setSessionDate(request.sessionDate());
        session.setRemark(request.remark());
        session.setLocked(false);

        AttendanceSession saved = attendanceSessionRepository.save(session);
        auditRecorder.record("ATTENDANCE_SESSION_CREATED", SESSION, saved.getId());

        return detail(saved, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW')")
    public AttendanceSessionDetailResponse getSession(UUID sessionId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();

        AttendanceSession session = loadSession(collegeId, sessionId);
        assertClassroomReadAccess(session.getClassroomId());

        return detail(session, recordsOf(collegeId, sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW')")
    public PageResponse<AttendanceSessionResponse> listSessions(UUID classroomId, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();

        classroomAccessService.requireClassroom(classroomId);
        assertClassroomReadAccess(classroomId);

        Page<AttendanceSession> sessions = attendanceSessionRepository
                .findAllByCollegeIdAndClassroomId(collegeId, classroomId, pageRequest(page, size));

        List<UUID> sessionIds = sessions.getContent().stream().map(AttendanceSession::getId).toList();
        Map<UUID, Map<AttendanceStatus, Long>> totalsBySession = sessionIds.isEmpty()
                ? Map.of()
                : groupBySession(attendanceRecordRepository.tallyBySessions(collegeId, sessionIds));

        Map<UUID, String> subjectNames = resolveSubjectNames(sessions.getContent().stream()
                .map(AttendanceSession::getSubjectId)
                .toList());

        return PageResponse.from(sessions, session -> attendanceMapper.toSessionResponse(session,
                subjectNames.get(session.getSubjectId()),
                AttendanceTotals.of(totalsBySession.getOrDefault(session.getId(), Map.of()))));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ATTENDANCE_UPDATE')")
    public AttendanceSessionResponse lockSession(UUID sessionId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();

        AttendanceSession session = loadSession(collegeId, sessionId);
        classroomAccessService.assertTeacher(session.getClassroomId());

        if (session.isLocked()) {
            throw new BusinessException("Attendance session is already locked");
        }

        session.setLocked(true);
        AttendanceSession saved = attendanceSessionRepository.save(session);
        auditRecorder.record("ATTENDANCE_SESSION_LOCKED", SESSION, saved.getId());

        return attendanceMapper.toSessionResponse(saved,
                subjectName(saved.getSubjectId()),
                totalsOf(recordsOf(collegeId, sessionId)));
    }

    // ------------------------------------------------------------------
    // Marking
    // ------------------------------------------------------------------

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    public AttendanceSessionDetailResponse markAttendance(UUID sessionId, MarkAttendanceRequest request) {
        return applyAttendance(sessionId, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ATTENDANCE_UPDATE')")
    public AttendanceSessionDetailResponse updateAttendance(UUID sessionId, MarkAttendanceRequest request) {
        return applyAttendance(sessionId, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ATTENDANCE_UPDATE')")
    public AttendanceRecordResponse updateRecord(UUID recordId, UpdateAttendanceRecordRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();

        AttendanceRecord attendanceRecord = attendanceRecordRepository.findByIdAndCollegeId(recordId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(RECORD, recordId));

        AttendanceSession session = attendanceRecord.getSession();
        classroomAccessService.assertTeacher(session.getClassroomId());

        if (session.isLocked()) {
            throw new BusinessException(LOCKED_MESSAGE);
        }

        attendanceRecord.setStatus(request.status());
        attendanceRecord.setRemark(request.remark());
        AttendanceRecord saved = attendanceRecordRepository.save(attendanceRecord);

        auditRecorder.record("ATTENDANCE_RECORD_UPDATED", RECORD, saved.getId());

        return attendanceMapper.toRecordResponse(saved,
                userNames(collegeId, List.of(saved.getStudentUserId())).get(saved.getStudentUserId()));
    }

    private AttendanceSessionDetailResponse applyAttendance(UUID sessionId, MarkAttendanceRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();

        AttendanceSession session = loadSession(collegeId, sessionId);
        classroomAccessService.assertTeacher(session.getClassroomId());

        if (session.isLocked()) {
            throw new BusinessException(LOCKED_MESSAGE);
        }

        // Later entries win when the same student is submitted twice.
        Map<String, AttendanceEntry> submitted = new LinkedHashMap<>();
        request.entries().forEach(entry -> submitted.put(entry.studentUserId().trim(), entry));

        Set<String> classroomStudents = new HashSet<>(
                classroomAccessService.studentUserIds(session.getClassroomId()));
        List<String> outsiders = submitted.keySet().stream()
                .filter(studentUserId -> !classroomStudents.contains(studentUserId))
                .toList();
        if (!outsiders.isEmpty()) {
            throw new BusinessException(
                    "Not students of this classroom: " + String.join(", ", outsiders));
        }

        Map<String, AttendanceRecord> existing = attendanceRecordRepository
                .findAllByCollegeIdAndSession_IdAndStudentUserIdIn(collegeId, sessionId, submitted.keySet())
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentUserId, attendanceRecord -> attendanceRecord));

        List<AttendanceRecord> upserts = new ArrayList<>(submitted.size());
        for (Map.Entry<String, AttendanceEntry> submission : submitted.entrySet()) {
            String studentUserId = submission.getKey();
            AttendanceEntry entry = submission.getValue();
            AttendanceRecord attendanceRecord = existing.get(studentUserId);

            if (attendanceRecord == null) {
                attendanceRecord = new AttendanceRecord();
                attendanceRecord.setCollegeId(collegeId);
                attendanceRecord.setSession(session);
                attendanceRecord.setStudentUserId(studentUserId);
            }
            attendanceRecord.setStatus(entry.status());
            attendanceRecord.setRemark(entry.remark());
            upserts.add(attendanceRecord);
        }
        attendanceRecordRepository.saveAll(upserts);

        auditRecorder.record("ATTENDANCE_MARKED", SESSION, sessionId);
        eventPublisher.publishEvent(new AttendanceMarkedEvent(collegeId,
                sessionId,
                session.getClassroomId(),
                List.copyOf(submitted.keySet()),
                Instant.now()));

        return detail(session, recordsOf(collegeId, sessionId));
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public MyAttendanceResponse myAttendance(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();
        String studentUserId = tenantGuard.requireUserId();

        if (classroomId != null) {
            classroomAccessService.assertMember(classroomId);
        }

        List<AttendanceRecord> history = classroomId == null
                ? attendanceRecordRepository.findStudentHistory(collegeId, studentUserId)
                : attendanceRecordRepository.findStudentHistoryInClassroom(collegeId, studentUserId, classroomId);

        List<SubjectStatusTally> tallies = classroomId == null
                ? attendanceRecordRepository.tallySubjectsOfStudent(collegeId, studentUserId)
                : attendanceRecordRepository.tallySubjectsOfStudentInClassroom(collegeId, studentUserId, classroomId);

        Map<UUID, Map<AttendanceStatus, Long>> countsBySubject = groupBySubject(tallies);
        Map<UUID, String> subjectNames = resolveSubjectNames(countsBySubject.keySet());

        List<SubjectAttendanceSummary> subjects = countsBySubject.entrySet().stream()
                .map(entry -> new SubjectAttendanceSummary(entry.getKey(),
                        subjectNames.get(entry.getKey()),
                        AttendanceTotals.of(entry.getValue())))
                .sorted(Comparator.comparing(SubjectAttendanceSummary::subjectName,
                        Comparator.nullsLast(Comparator.<String>naturalOrder())))
                .toList();

        Map<AttendanceStatus, Long> overall = new EnumMap<>(AttendanceStatus.class);
        countsBySubject.values()
                .forEach(counts -> counts.forEach((status, count) -> overall.merge(status, count, Long::sum)));

        List<AttendanceHistoryItem> items = history.stream()
                .map(attendanceRecord -> attendanceMapper.toHistoryItem(attendanceRecord,
                        subjectNames.get(attendanceRecord.getSession().getSubjectId())))
                .toList();

        return new MyAttendanceResponse(studentUserId,
                classroomId,
                AttendanceTotals.of(overall),
                subjects,
                items);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendancePercentageResponse attendancePercentage(UUID classroomId, String studentUserId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();
        String currentUserId = tenantGuard.requireUserId();

        boolean ownAttendance = currentUserId.equals(studentUserId);
        if (!ownAttendance && !hasAuthority(Permissions.ATTENDANCE_VIEW)) {
            throw new ForbiddenException("You may only view your own attendance");
        }

        classroomAccessService.requireClassroom(classroomId);
        if (ownAttendance) {
            classroomAccessService.assertMember(classroomId);
        } else {
            assertClassroomReadAccess(classroomId);
            if (!classroomAccessService.isMember(classroomId, studentUserId)) {
                throw new ResourceNotFoundException("Classroom member", studentUserId);
            }
        }

        AttendanceTotals totals = AttendanceTotals.of(statusCounts(
                attendanceRecordRepository.tallyByClassroomAndStudent(collegeId, classroomId, studentUserId)));

        return new AttendancePercentageResponse(classroomId, studentUserId, totals);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW')")
    public ClassroomAttendanceReport classroomReport(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ATTENDANCE);
        UUID collegeId = tenantGuard.requireCollegeId();

        classroomAccessService.requireClassroom(classroomId);
        assertClassroomReadAccess(classroomId);

        Map<String, Map<AttendanceStatus, Long>> countsByStudent =
                groupByStudent(attendanceRecordRepository.tallyByClassroom(collegeId, classroomId));

        List<String> studentUserIds = classroomAccessService.studentUserIds(classroomId);
        Map<String, String> studentNames = userNames(collegeId, studentUserIds);

        List<StudentAttendanceReportRow> rows = studentUserIds.stream()
                .map(studentUserId -> new StudentAttendanceReportRow(studentUserId,
                        studentNames.get(studentUserId),
                        AttendanceTotals.of(countsByStudent.getOrDefault(studentUserId, Map.of()))))
                .toList();

        return new ClassroomAttendanceReport(classroomId,
                attendanceSessionRepository.countByCollegeIdAndClassroomId(collegeId, classroomId),
                rows);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private AttendanceSession loadSession(UUID collegeId, UUID sessionId) {
        return attendanceSessionRepository.findByIdAndCollegeId(sessionId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION, sessionId));
    }

    private List<AttendanceRecord> recordsOf(UUID collegeId, UUID sessionId) {
        return attendanceRecordRepository
                .findAllByCollegeIdAndSession_IdOrderByStudentUserIdAsc(collegeId, sessionId);
    }

    private void assertSessionSlotFree(UUID collegeId, UUID classroomId, LocalDate sessionDate, UUID subjectId) {
        boolean taken = subjectId == null
                ? attendanceSessionRepository
                .existsByCollegeIdAndClassroomIdAndSessionDateAndSubjectIdIsNull(collegeId, classroomId, sessionDate)
                : attendanceSessionRepository
                .existsByCollegeIdAndClassroomIdAndSessionDateAndSubjectId(collegeId, classroomId, sessionDate, subjectId);

        if (taken) {
            throw new DuplicateResourceException(
                    "An attendance session already exists for this classroom, date and subject");
        }
    }

    /**
     * Aggregate reads are allowed for classroom members; a college
     * administrator does not have to join a classroom to audit it.
     */
    private void assertClassroomReadAccess(UUID classroomId) {
        if (tenantGuard.isMainAdmin() || hasAuthority("ROLE_" + RoleNames.COLLEGE_ADMIN)) {
            return;
        }
        classroomAccessService.assertMember(classroomId);
    }

    private AttendanceSessionDetailResponse detail(AttendanceSession session,
                                                   List<AttendanceRecord> attendanceRecords) {
        Map<String, String> studentNames = userNames(session.getCollegeId(),
                attendanceRecords.stream().map(AttendanceRecord::getStudentUserId).toList());

        return attendanceMapper.toSessionDetailResponse(session,
                subjectName(session.getSubjectId()),
                totalsOf(attendanceRecords),
                attendanceMapper.toRecordResponses(attendanceRecords, studentNames));
    }

    private static AttendanceTotals totalsOf(List<AttendanceRecord> attendanceRecords) {
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        attendanceRecords.forEach(attendanceRecord ->
                counts.merge(attendanceRecord.getStatus(), 1L, Long::sum));
        return AttendanceTotals.of(counts);
    }

    private String subjectName(UUID subjectId) {
        if (subjectId == null || !academicLookupService.subjectExists(subjectId)) {
            return null;
        }
        return academicLookupService.subjectName(subjectId);
    }

    /** Mutable map on purpose: subject ids may be null for whole-classroom sessions. */
    private Map<UUID, String> resolveSubjectNames(Collection<UUID> subjectIds) {
        Map<UUID, String> names = new HashMap<>();
        subjectIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(academicLookupService::subjectExists)
                .forEach(subjectId -> names.put(subjectId, academicLookupService.subjectName(subjectId)));
        return names;
    }

    private Map<String, String> userNames(UUID collegeId, Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepo.findAllById(userIds).stream()
                .filter(user -> collegeId.equals(user.getCollegeId()))
                .collect(Collectors.toMap(UserEntity::getUserId, UserEntity::getUserName));
    }

    private static Map<AttendanceStatus, Long> statusCounts(List<AttendanceStatusTally> tallies) {
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        tallies.forEach(tally -> counts.merge(tally.status(), tally.count(), Long::sum));
        return counts;
    }

    private static Map<String, Map<AttendanceStatus, Long>> groupByStudent(List<AttendanceStatusTally> tallies) {
        Map<String, Map<AttendanceStatus, Long>> grouped = new HashMap<>();
        tallies.forEach(tally -> grouped
                .computeIfAbsent(tally.studentUserId(), key -> new EnumMap<>(AttendanceStatus.class))
                .merge(tally.status(), tally.count(), Long::sum));
        return grouped;
    }

    private static Map<UUID, Map<AttendanceStatus, Long>> groupBySubject(List<SubjectStatusTally> tallies) {
        Map<UUID, Map<AttendanceStatus, Long>> grouped = new HashMap<>();
        tallies.forEach(tally -> grouped
                .computeIfAbsent(tally.subjectId(), key -> new EnumMap<>(AttendanceStatus.class))
                .merge(tally.status(), tally.count(), Long::sum));
        return grouped;
    }

    private static Map<UUID, Map<AttendanceStatus, Long>> groupBySession(List<SessionStatusTally> tallies) {
        Map<UUID, Map<AttendanceStatus, Long>> grouped = new HashMap<>();
        tallies.forEach(tally -> grouped
                .computeIfAbsent(tally.sessionId(), key -> new EnumMap<>(AttendanceStatus.class))
                .merge(tally.status(), tally.count(), Long::sum));
        return grouped;
    }

    private static Pageable pageRequest(int page, int size) {
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Order.desc("sessionDate"), Sort.Order.desc("createdAt")));
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
