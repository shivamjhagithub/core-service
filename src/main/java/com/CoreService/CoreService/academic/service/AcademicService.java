package com.CoreService.CoreService.academic.service;

import com.CoreService.CoreService.academic.dto.AcademicSessionRequest;
import com.CoreService.CoreService.academic.dto.AcademicSessionResponse;
import com.CoreService.CoreService.academic.dto.DepartmentRequest;
import com.CoreService.CoreService.academic.dto.DepartmentResponse;
import com.CoreService.CoreService.academic.dto.ProgramRequest;
import com.CoreService.CoreService.academic.dto.ProgramResponse;
import com.CoreService.CoreService.academic.dto.SemesterRequest;
import com.CoreService.CoreService.academic.dto.SemesterResponse;
import com.CoreService.CoreService.academic.dto.SubjectRequest;
import com.CoreService.CoreService.academic.dto.SubjectResponse;
import com.CoreService.CoreService.academic.entity.AcademicSession;
import com.CoreService.CoreService.academic.entity.Department;
import com.CoreService.CoreService.academic.entity.Program;
import com.CoreService.CoreService.academic.entity.Semester;
import com.CoreService.CoreService.academic.entity.Subject;
import com.CoreService.CoreService.academic.mapper.AcademicMapper;
import com.CoreService.CoreService.academic.repository.AcademicSessionRepository;
import com.CoreService.CoreService.academic.repository.DepartmentRepository;
import com.CoreService.CoreService.academic.repository.ProgramRepository;
import com.CoreService.CoreService.academic.repository.SemesterRepository;
import com.CoreService.CoreService.academic.repository.SubjectRepository;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.DuplicateResourceException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Owns the academic structure of a college: departments, programs, sessions,
 * semesters and subjects.
 * <p>
 * Two invariants are enforced here rather than in the schema, because they span
 * rows: a parent always belongs to the caller's college, and at most one session
 * per college is flagged as current.
 */
@Service
@RequiredArgsConstructor
public class AcademicService {

    private static final String DEPARTMENT = "Department";
    private static final String PROGRAM = "Program";
    private static final String SESSION = "Academic session";
    private static final String SEMESTER = "Semester";
    private static final String SUBJECT = "Subject";

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicMapper academicMapper;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;

    // ------------------------------------------------------------------
    // Departments
    // ------------------------------------------------------------------

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        String code = trimmed(request.code());
        if (departmentRepository.existsByCollegeIdAndCodeIgnoreCase(collegeId, code)) {
            throw new DuplicateResourceException("A department with code " + code + " already exists");
        }

        Department department = new Department();
        department.setCollegeId(collegeId);
        department.setName(trimmed(request.name()));
        department.setCode(code);
        department.setDescription(request.description());

        return academicMapper.toDepartmentResponse(departmentRepository.save(department));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Department department = requireDepartment(departmentId, collegeId);
        String code = trimmed(request.code());
        if (departmentRepository.existsByCollegeIdAndCodeIgnoreCaseAndIdNot(collegeId, code, departmentId)) {
            throw new DuplicateResourceException("A department with code " + code + " already exists");
        }

        department.setName(trimmed(request.name()));
        department.setCode(code);
        department.setDescription(request.description());

        return academicMapper.toDepartmentResponse(departmentRepository.saveAndFlush(department));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public void deleteDepartment(UUID departmentId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Department department = requireDepartment(departmentId, collegeId);
        if (programRepository.existsByCollegeIdAndDepartment_Id(collegeId, departmentId)) {
            throw new BusinessException("Department cannot be deleted while it still has programs");
        }

        departmentRepository.delete(department);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public DepartmentResponse getDepartment(UUID departmentId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        Department department = requireDepartment(departmentId, tenantGuard.requireCollegeId());

        return academicMapper.toDepartmentResponse(department);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public PageResponse<DepartmentResponse> listDepartments(int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        return PageResponse.from(
                departmentRepository.findAllByCollegeId(collegeId, pageable(page, size, Sort.by("name"))),
                academicMapper::toDepartmentResponse);
    }

    // ------------------------------------------------------------------
    // Programs
    // ------------------------------------------------------------------

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public ProgramResponse createProgram(ProgramRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        String code = trimmed(request.code());
        if (programRepository.existsByCollegeIdAndCodeIgnoreCase(collegeId, code)) {
            throw new DuplicateResourceException("A program with code " + code + " already exists");
        }

        Program program = new Program();
        program.setCollegeId(collegeId);
        program.setName(trimmed(request.name()));
        program.setCode(code);
        program.setDurationYears(request.durationYears());
        program.setDescription(request.description());
        program.setDepartment(requireDepartment(request.departmentId(), collegeId));

        return academicMapper.toProgramResponse(programRepository.save(program));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public ProgramResponse updateProgram(UUID programId, ProgramRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Program program = requireProgram(programId, collegeId);
        String code = trimmed(request.code());
        if (programRepository.existsByCollegeIdAndCodeIgnoreCaseAndIdNot(collegeId, code, programId)) {
            throw new DuplicateResourceException("A program with code " + code + " already exists");
        }

        program.setName(trimmed(request.name()));
        program.setCode(code);
        program.setDurationYears(request.durationYears());
        program.setDescription(request.description());
        program.setDepartment(requireDepartment(request.departmentId(), collegeId));

        return academicMapper.toProgramResponse(programRepository.saveAndFlush(program));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public void deleteProgram(UUID programId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Program program = requireProgram(programId, collegeId);
        if (semesterRepository.existsByCollegeIdAndProgram_Id(collegeId, programId)) {
            throw new BusinessException("Program cannot be deleted while it still has semesters");
        }

        programRepository.delete(program);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public ProgramResponse getProgram(UUID programId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        Program program = requireProgram(programId, tenantGuard.requireCollegeId());

        return academicMapper.toProgramResponse(program);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public PageResponse<ProgramResponse> listPrograms(UUID departmentId, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();
        Pageable pageable = pageable(page, size, Sort.by("name"));

        if (departmentId == null) {
            return PageResponse.from(
                    programRepository.findAllByCollegeId(collegeId, pageable),
                    academicMapper::toProgramResponse);
        }

        requireDepartment(departmentId, collegeId);
        return PageResponse.from(
                programRepository.findAllByCollegeIdAndDepartment_Id(collegeId, departmentId, pageable),
                academicMapper::toProgramResponse);
    }

    // ------------------------------------------------------------------
    // Academic sessions
    // ------------------------------------------------------------------

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public AcademicSessionResponse createSession(AcademicSessionRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        String name = trimmed(request.name());
        assertEndsAfterStart(request.startDate(), request.endDate());
        if (academicSessionRepository.existsByCollegeIdAndNameIgnoreCase(collegeId, name)) {
            throw new DuplicateResourceException("A session named " + name + " already exists");
        }

        if (request.current()) {
            demoteOtherCurrentSessions(collegeId, null);
        }

        AcademicSession session = new AcademicSession();
        session.setCollegeId(collegeId);
        session.setName(name);
        session.setStartDate(request.startDate());
        session.setEndDate(request.endDate());
        session.setCurrent(request.current());

        return academicMapper.toSessionResponse(academicSessionRepository.save(session));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public AcademicSessionResponse updateSession(UUID sessionId, AcademicSessionRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        AcademicSession session = requireSession(sessionId, collegeId);
        String name = trimmed(request.name());
        assertEndsAfterStart(request.startDate(), request.endDate());
        if (academicSessionRepository.existsByCollegeIdAndNameIgnoreCaseAndIdNot(collegeId, name, sessionId)) {
            throw new DuplicateResourceException("A session named " + name + " already exists");
        }

        if (request.current()) {
            demoteOtherCurrentSessions(collegeId, sessionId);
        }

        session.setName(name);
        session.setStartDate(request.startDate());
        session.setEndDate(request.endDate());
        session.setCurrent(request.current());

        return academicMapper.toSessionResponse(academicSessionRepository.saveAndFlush(session));
    }

    /**
     * Promotes one session and demotes the rest in the same transaction, so the
     * college is never left with two current sessions.
     */
    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public AcademicSessionResponse markSessionAsCurrent(UUID sessionId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        AcademicSession session = requireSession(sessionId, collegeId);
        demoteOtherCurrentSessions(collegeId, sessionId);
        session.setCurrent(true);

        return academicMapper.toSessionResponse(academicSessionRepository.saveAndFlush(session));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public void deleteSession(UUID sessionId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        AcademicSession session = requireSession(sessionId, collegeId);
        if (semesterRepository.existsByCollegeIdAndAcademicSession_Id(collegeId, sessionId)) {
            throw new BusinessException("Session cannot be deleted while semesters are attached to it");
        }

        academicSessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public AcademicSessionResponse getSession(UUID sessionId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        AcademicSession session = requireSession(sessionId, tenantGuard.requireCollegeId());

        return academicMapper.toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public AcademicSessionResponse getCurrentSession() {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        return academicSessionRepository.findByCollegeIdAndCurrentTrue(collegeId)
                .map(academicMapper::toSessionResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No current academic session is configured"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public PageResponse<AcademicSessionResponse> listSessions(int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        return PageResponse.from(
                academicSessionRepository.findAllByCollegeId(
                        collegeId, pageable(page, size, Sort.by(Sort.Direction.DESC, "startDate"))),
                academicMapper::toSessionResponse);
    }

    // ------------------------------------------------------------------
    // Semesters
    // ------------------------------------------------------------------

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public SemesterResponse createSemester(SemesterRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Program program = requireProgram(request.programId(), collegeId);
        if (semesterRepository.existsByCollegeIdAndProgram_IdAndNumber(
                collegeId, request.programId(), request.number())) {
            throw new DuplicateResourceException(
                    "Semester " + request.number() + " already exists for this program");
        }

        Semester semester = new Semester();
        semester.setCollegeId(collegeId);
        semester.setNumber(request.number());
        semester.setName(trimmed(request.name()));
        semester.setProgram(program);
        semester.setAcademicSession(resolveSession(request.academicSessionId(), collegeId));

        return academicMapper.toSemesterResponse(semesterRepository.save(semester));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public SemesterResponse updateSemester(UUID semesterId, SemesterRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Semester semester = requireSemester(semesterId, collegeId);
        Program program = requireProgram(request.programId(), collegeId);
        if (semesterRepository.existsByCollegeIdAndProgram_IdAndNumberAndIdNot(
                collegeId, request.programId(), request.number(), semesterId)) {
            throw new DuplicateResourceException(
                    "Semester " + request.number() + " already exists for this program");
        }

        semester.setNumber(request.number());
        semester.setName(trimmed(request.name()));
        semester.setProgram(program);
        semester.setAcademicSession(resolveSession(request.academicSessionId(), collegeId));

        return academicMapper.toSemesterResponse(semesterRepository.saveAndFlush(semester));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public void deleteSemester(UUID semesterId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Semester semester = requireSemester(semesterId, collegeId);
        if (subjectRepository.existsByCollegeIdAndSemester_Id(collegeId, semesterId)) {
            throw new BusinessException("Semester cannot be deleted while it still has subjects");
        }

        semesterRepository.delete(semester);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public SemesterResponse getSemester(UUID semesterId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        Semester semester = requireSemester(semesterId, tenantGuard.requireCollegeId());

        return academicMapper.toSemesterResponse(semester);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public PageResponse<SemesterResponse> listSemesters(UUID programId, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();
        Pageable pageable = pageable(page, size, Sort.by("number"));

        if (programId == null) {
            return PageResponse.from(
                    semesterRepository.findAllByCollegeId(collegeId, pageable),
                    academicMapper::toSemesterResponse);
        }

        requireProgram(programId, collegeId);
        return PageResponse.from(
                semesterRepository.findAllByCollegeIdAndProgram_Id(collegeId, programId, pageable),
                academicMapper::toSemesterResponse);
    }

    // ------------------------------------------------------------------
    // Subjects
    // ------------------------------------------------------------------

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public SubjectResponse createSubject(SubjectRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Semester semester = requireSemester(request.semesterId(), collegeId);
        String code = trimmed(request.code());
        if (subjectRepository.existsByCollegeIdAndCodeIgnoreCase(collegeId, code)) {
            throw new DuplicateResourceException("A subject with code " + code + " already exists");
        }

        Subject subject = new Subject();
        subject.setCollegeId(collegeId);
        subject.setName(trimmed(request.name()));
        subject.setCode(code);
        subject.setCredits(request.credits());
        subject.setDescription(request.description());
        subject.setSemester(semester);

        return academicMapper.toSubjectResponse(subjectRepository.save(subject));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public SubjectResponse updateSubject(UUID subjectId, SubjectRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        Subject subject = requireSubject(subjectId, collegeId);
        Semester semester = requireSemester(request.semesterId(), collegeId);
        String code = trimmed(request.code());
        if (subjectRepository.existsByCollegeIdAndCodeIgnoreCaseAndIdNot(collegeId, code, subjectId)) {
            throw new DuplicateResourceException("A subject with code " + code + " already exists");
        }

        subject.setName(trimmed(request.name()));
        subject.setCode(code);
        subject.setCredits(request.credits());
        subject.setDescription(request.description());
        subject.setSemester(semester);

        return academicMapper.toSubjectResponse(subjectRepository.saveAndFlush(subject));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    public void deleteSubject(UUID subjectId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();

        subjectRepository.delete(requireSubject(subjectId, collegeId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public SubjectResponse getSubject(UUID subjectId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        Subject subject = requireSubject(subjectId, tenantGuard.requireCollegeId());

        return academicMapper.toSubjectResponse(subject);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    public PageResponse<SubjectResponse> listSubjects(UUID semesterId, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ACADEMIC);
        UUID collegeId = tenantGuard.requireCollegeId();
        Pageable pageable = pageable(page, size, Sort.by("name"));

        if (semesterId == null) {
            return PageResponse.from(
                    subjectRepository.findAllByCollegeId(collegeId, pageable),
                    academicMapper::toSubjectResponse);
        }

        requireSemester(semesterId, collegeId);
        return PageResponse.from(
                subjectRepository.findAllByCollegeIdAndSemester_Id(collegeId, semesterId, pageable),
                academicMapper::toSubjectResponse);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private Department requireDepartment(UUID departmentId, UUID collegeId) {
        Department department = departmentRepository.findByIdAndCollegeId(departmentId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(DEPARTMENT, departmentId));
        return tenantGuard.requireOwned(department, DEPARTMENT);
    }

    private Program requireProgram(UUID programId, UUID collegeId) {
        Program program = programRepository.findByIdAndCollegeId(programId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(PROGRAM, programId));
        return tenantGuard.requireOwned(program, PROGRAM);
    }

    private AcademicSession requireSession(UUID sessionId, UUID collegeId) {
        AcademicSession session = academicSessionRepository.findByIdAndCollegeId(sessionId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION, sessionId));
        return tenantGuard.requireOwned(session, SESSION);
    }

    private Semester requireSemester(UUID semesterId, UUID collegeId) {
        Semester semester = semesterRepository.findByIdAndCollegeId(semesterId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(SEMESTER, semesterId));
        return tenantGuard.requireOwned(semester, SEMESTER);
    }

    private Subject requireSubject(UUID subjectId, UUID collegeId) {
        Subject subject = subjectRepository.findByIdAndCollegeId(subjectId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT, subjectId));
        return tenantGuard.requireOwned(subject, SUBJECT);
    }

    private AcademicSession resolveSession(UUID sessionId, UUID collegeId) {
        return sessionId == null ? null : requireSession(sessionId, collegeId);
    }

    /**
     * The demoted sessions stay managed, so their updates and the promotion flush
     * as a single unit of work. A null {@code promotedSessionId} demotes them all,
     * which is the case when the session being promoted is not persisted yet.
     */
    private void demoteOtherCurrentSessions(UUID collegeId, UUID promotedSessionId) {
        academicSessionRepository.findAllByCollegeIdAndCurrentTrue(collegeId).stream()
                .filter(other -> !other.getId().equals(promotedSessionId))
                .forEach(other -> other.setCurrent(false));
    }

    private void assertEndsAfterStart(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new BusinessException("Session end date must be after its start date");
        }
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, sort);
    }

    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
