package com.CoreService.CoreService.teacher.service;

import com.CoreService.CoreService.academic.service.AcademicLookupService;
import com.CoreService.CoreService.common.exception.DuplicateResourceException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.teacher.dto.TeacherRequest;
import com.CoreService.CoreService.teacher.dto.TeacherResponse;
import com.CoreService.CoreService.teacher.dto.TeacherSubjectResponse;
import com.CoreService.CoreService.teacher.dto.TeacherUpdateRequest;
import com.CoreService.CoreService.teacher.entity.Teacher;
import com.CoreService.CoreService.teacher.entity.TeacherSubject;
import com.CoreService.CoreService.teacher.mapper.TeacherMapper;
import com.CoreService.CoreService.teacher.repository.TeacherRepository;
import com.CoreService.CoreService.teacher.repository.TeacherSubjectRepository;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Teacher records of a college and the subjects they teach. A record can only be
 * attached to a user that already exists inside the caller's college, and a
 * subject assignment is accepted only after the academic module confirms the
 * subject belongs to that same college.
 */
@Service
@RequiredArgsConstructor
public class TeacherService {

    private static final String TEACHER = "Teacher";
    private static final String USER = "User";
    private static final String TEACHER_SUBJECT = "Subject assignment";

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TeacherRepository teacherRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherMapper teacherMapper;
    private final UserRepo userRepo;
    private final AcademicLookupService academicLookupService;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;

    @Transactional
    @PreAuthorize("hasAuthority('TEACHER_CREATE')")
    public TeacherResponse createTeacher(TeacherRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        String userId = request.userId().trim();
        userRepo.findByUserIdAndCollegeId(userId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(USER, userId));

        if (teacherRepository.existsByCollegeIdAndUserId(collegeId, userId)) {
            throw new DuplicateResourceException("A teacher record already exists for user " + userId);
        }

        String employeeId = normalized(request.employeeId());
        assertEmployeeIdAvailable(collegeId, employeeId, null);

        Teacher teacher = new Teacher();
        teacher.setCollegeId(collegeId);
        teacher.setUserId(userId);
        teacher.setEmployeeId(employeeId);
        teacher.setDesignation(normalized(request.designation()));
        teacher.setQualification(normalized(request.qualification()));
        teacher.setDepartmentId(request.departmentId());
        teacher.setActive(true);

        return teacherMapper.toTeacherResponse(teacherRepository.save(teacher));
    }

    @Transactional
    @PreAuthorize("hasAuthority('TEACHER_UPDATE')")
    public TeacherResponse updateTeacher(UUID teacherId, TeacherUpdateRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        Teacher teacher = requireTeacher(teacherId, collegeId);
        String employeeId = normalized(request.employeeId());
        assertEmployeeIdAvailable(collegeId, employeeId, teacherId);

        teacher.setEmployeeId(employeeId);
        teacher.setDesignation(normalized(request.designation()));
        teacher.setQualification(normalized(request.qualification()));
        teacher.setDepartmentId(request.departmentId());

        return teacherMapper.toTeacherResponse(teacherRepository.saveAndFlush(teacher));
    }

    @Transactional
    @PreAuthorize("hasAuthority('TEACHER_UPDATE')")
    public TeacherResponse activateTeacher(UUID teacherId) {
        return changeActiveFlag(teacherId, true);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TEACHER_UPDATE')")
    public TeacherResponse deactivateTeacher(UUID teacherId) {
        return changeActiveFlag(teacherId, false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TEACHER_VIEW')")
    public TeacherResponse getTeacher(UUID teacherId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        Teacher teacher = requireTeacher(teacherId, tenantGuard.requireCollegeId());

        return teacherMapper.toTeacherResponse(teacher);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TEACHER_VIEW')")
    public TeacherResponse getTeacherByUserId(String userId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        Teacher teacher = requireTeacherByUserId(userId, tenantGuard.requireCollegeId());

        return teacherMapper.toTeacherResponse(teacher);
    }

    /**
     * Profile of the signed-in teacher. Authentication alone is enough because
     * the record is resolved from the token, never from a client-supplied id.
     */
    @Transactional(readOnly = true)
    public TeacherResponse getMyProfile() {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        Teacher teacher = requireTeacherByUserId(tenantGuard.requireUserId(), collegeId);

        return teacherMapper.toTeacherResponse(teacher);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TEACHER_VIEW')")
    public PageResponse<TeacherResponse> listTeachers(int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        return PageResponse.from(
                teacherRepository.findAllByCollegeId(collegeId, pageable(page, size)),
                teacherMapper::toTeacherResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TEACHER_VIEW')")
    public PageResponse<TeacherResponse> searchTeachers(String keyword, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        String term = normalized(keyword);
        Pageable pageable = pageable(page, size);
        Page<Teacher> results = term == null
                ? teacherRepository.findAllByCollegeId(collegeId, pageable)
                : teacherRepository.search(collegeId, term, pageable);

        return PageResponse.from(results, teacherMapper::toTeacherResponse);
    }

    // ------------------------------------------------------------------
    // Subject assignments
    // ------------------------------------------------------------------

    @Transactional
    @PreAuthorize("hasAuthority('TEACHER_UPDATE')")
    public TeacherSubjectResponse assignSubject(UUID teacherId, UUID subjectId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        Teacher teacher = requireTeacher(teacherId, collegeId);
        academicLookupService.requireSubject(subjectId);

        if (teacherSubjectRepository.existsByCollegeIdAndTeacher_IdAndSubjectId(collegeId, teacherId, subjectId)) {
            throw new DuplicateResourceException("This subject is already assigned to the teacher");
        }

        TeacherSubject assignment = new TeacherSubject();
        assignment.setCollegeId(collegeId);
        assignment.setTeacher(teacher);
        assignment.setSubjectId(subjectId);

        return teacherMapper.toTeacherSubjectResponse(teacherSubjectRepository.save(assignment));
    }

    @Transactional
    @PreAuthorize("hasAuthority('TEACHER_UPDATE')")
    public void removeSubject(UUID teacherId, UUID subjectId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        requireTeacher(teacherId, collegeId);
        TeacherSubject assignment = teacherSubjectRepository
                .findByCollegeIdAndTeacher_IdAndSubjectId(collegeId, teacherId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_SUBJECT, subjectId));

        teacherSubjectRepository.delete(tenantGuard.requireOwned(assignment, TEACHER_SUBJECT));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TEACHER_VIEW')")
    public List<TeacherSubjectResponse> listAssignedSubjects(UUID teacherId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        requireTeacher(teacherId, collegeId);
        return teacherSubjectRepository.findAllByCollegeIdAndTeacher_Id(collegeId, teacherId).stream()
                .map(teacherMapper::toTeacherSubjectResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private TeacherResponse changeActiveFlag(UUID teacherId, boolean active) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        Teacher teacher = requireTeacher(teacherId, collegeId);
        teacher.setActive(active);

        return teacherMapper.toTeacherResponse(teacherRepository.saveAndFlush(teacher));
    }

    private Teacher requireTeacher(UUID teacherId, UUID collegeId) {
        Teacher teacher = teacherRepository.findByIdAndCollegeId(teacherId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER, teacherId));
        return tenantGuard.requireOwned(teacher, TEACHER);
    }

    private Teacher requireTeacherByUserId(String userId, UUID collegeId) {
        Teacher teacher = teacherRepository.findByCollegeIdAndUserId(collegeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER, userId));
        return tenantGuard.requireOwned(teacher, TEACHER);
    }

    private void assertEmployeeIdAvailable(UUID collegeId, String employeeId, UUID excludedTeacherId) {
        if (employeeId == null) {
            return;
        }

        boolean taken = excludedTeacherId == null
                ? teacherRepository.existsByCollegeIdAndEmployeeIdIgnoreCase(collegeId, employeeId)
                : teacherRepository.existsByCollegeIdAndEmployeeIdIgnoreCaseAndIdNot(
                        collegeId, employeeId, excludedTeacherId);

        if (taken) {
            throw new DuplicateResourceException("Employee id " + employeeId + " is already assigned");
        }
    }

    private Pageable pageable(int page, int size) {
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by("employeeId"));
    }

    /** Blank optional identifiers become null so they stay out of the unique index. */
    private static String normalized(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
