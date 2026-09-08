package com.CoreService.CoreService.student.service;

import com.CoreService.CoreService.common.exception.DuplicateResourceException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.student.dto.StudentRequest;
import com.CoreService.CoreService.student.dto.StudentResponse;
import com.CoreService.CoreService.student.dto.StudentUpdateRequest;
import com.CoreService.CoreService.student.entity.Student;
import com.CoreService.CoreService.student.mapper.StudentMapper;
import com.CoreService.CoreService.student.repository.StudentRepository;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Student records of a college. A record can only be attached to a user that
 * already exists inside the caller's college, which is what keeps the students
 * table from referencing people of another tenant.
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String STUDENT = "Student";
    private static final String USER = "User";

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final UserRepo userRepo;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;

    @Transactional
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public StudentResponse createStudent(StudentRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        String userId = request.userId().trim();
        userRepo.findByUserIdAndCollegeId(userId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(USER, userId));

        if (studentRepository.existsByCollegeIdAndUserId(collegeId, userId)) {
            throw new DuplicateResourceException("A student record already exists for user " + userId);
        }

        String rollNumber = normalized(request.rollNumber());
        String registrationNumber = normalized(request.registrationNumber());
        assertRollNumberAvailable(collegeId, rollNumber, null);
        assertRegistrationNumberAvailable(collegeId, registrationNumber, null);

        Student student = new Student();
        student.setCollegeId(collegeId);
        student.setUserId(userId);
        student.setRollNumber(rollNumber);
        student.setRegistrationNumber(registrationNumber);
        student.setFatherName(normalized(request.fatherName()));
        student.setBloodGroup(normalized(request.bloodGroup()));
        student.setSemester(request.semester());
        student.setDepartmentId(request.departmentId());
        student.setProgramId(request.programId());
        student.setActive(true);

        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public StudentResponse updateStudent(UUID studentId, StudentUpdateRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        Student student = requireStudent(studentId, collegeId);
        String rollNumber = normalized(request.rollNumber());
        String registrationNumber = normalized(request.registrationNumber());
        assertRollNumberAvailable(collegeId, rollNumber, studentId);
        assertRegistrationNumberAvailable(collegeId, registrationNumber, studentId);

        student.setRollNumber(rollNumber);
        student.setRegistrationNumber(registrationNumber);
        student.setFatherName(normalized(request.fatherName()));
        student.setBloodGroup(normalized(request.bloodGroup()));
        student.setSemester(request.semester());
        student.setDepartmentId(request.departmentId());
        student.setProgramId(request.programId());

        return studentMapper.toResponse(studentRepository.saveAndFlush(student));
    }

    @Transactional
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public StudentResponse activateStudent(UUID studentId) {
        return changeActiveFlag(studentId, true);
    }

    @Transactional
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public StudentResponse deactivateStudent(UUID studentId) {
        return changeActiveFlag(studentId, false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public StudentResponse getStudent(UUID studentId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        return studentMapper.toResponse(requireStudent(studentId, tenantGuard.requireCollegeId()));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public StudentResponse getStudentByUserId(String userId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        return studentMapper.toResponse(requireStudentByUserId(userId, tenantGuard.requireCollegeId()));
    }

    /**
     * Profile of the signed-in student. Authentication alone is enough because
     * the record is resolved from the token, never from a client-supplied id.
     */
    @Transactional(readOnly = true)
    public StudentResponse getMyProfile() {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        return studentMapper.toResponse(requireStudentByUserId(tenantGuard.requireUserId(), collegeId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public PageResponse<StudentResponse> listStudents(int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        return PageResponse.from(
                studentRepository.findAllByCollegeId(collegeId, pageable(page, size)),
                studentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public PageResponse<StudentResponse> searchStudents(String keyword, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        String term = normalized(keyword);
        Pageable pageable = pageable(page, size);
        Page<Student> results = term == null
                ? studentRepository.findAllByCollegeId(collegeId, pageable)
                : studentRepository.search(collegeId, term, pageable);

        return PageResponse.from(results, studentMapper::toResponse);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private StudentResponse changeActiveFlag(UUID studentId, boolean active) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.USER_MANAGEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        Student student = requireStudent(studentId, collegeId);
        student.setActive(active);

        return studentMapper.toResponse(studentRepository.saveAndFlush(student));
    }

    private Student requireStudent(UUID studentId, UUID collegeId) {
        Student student = studentRepository.findByIdAndCollegeId(studentId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT, studentId));
        return tenantGuard.requireOwned(student, STUDENT);
    }

    private Student requireStudentByUserId(String userId, UUID collegeId) {
        Student student = studentRepository.findByCollegeIdAndUserId(collegeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT, userId));
        return tenantGuard.requireOwned(student, STUDENT);
    }

    private void assertRollNumberAvailable(UUID collegeId, String rollNumber, UUID excludedStudentId) {
        if (rollNumber == null) {
            return;
        }

        boolean taken = excludedStudentId == null
                ? studentRepository.existsByCollegeIdAndRollNumberIgnoreCase(collegeId, rollNumber)
                : studentRepository.existsByCollegeIdAndRollNumberIgnoreCaseAndIdNot(
                        collegeId, rollNumber, excludedStudentId);

        if (taken) {
            throw new DuplicateResourceException("Roll number " + rollNumber + " is already assigned");
        }
    }

    private void assertRegistrationNumberAvailable(UUID collegeId, String registrationNumber, UUID excludedStudentId) {
        if (registrationNumber == null) {
            return;
        }

        boolean taken = excludedStudentId == null
                ? studentRepository.existsByCollegeIdAndRegistrationNumberIgnoreCase(collegeId, registrationNumber)
                : studentRepository.existsByCollegeIdAndRegistrationNumberIgnoreCaseAndIdNot(
                        collegeId, registrationNumber, excludedStudentId);

        if (taken) {
            throw new DuplicateResourceException(
                    "Registration number " + registrationNumber + " is already assigned");
        }
    }

    private Pageable pageable(int page, int size) {
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by("rollNumber"));
    }

    /** Blank optional identifiers become null so they stay out of the unique indexes. */
    private static String normalized(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
