package com.CoreService.CoreService.student.service;

import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.student.entity.Student;
import com.CoreService.CoreService.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cross-module student lookups. Both methods answer for the caller's college
 * only and stay silent (false / null) when there is no college context, because
 * callers use them to branch, not to authorize.
 */
@Service
@RequiredArgsConstructor
public class StudentLookupServiceImpl implements StudentLookupService {

    private final StudentRepository studentRepository;
    private final TenantGuard tenantGuard;

    @Override
    @Transactional(readOnly = true)
    public boolean isStudent(String userId) {
        UUID collegeId = tenantGuard.currentCollegeId();
        if (userId == null || collegeId == null) {
            return false;
        }

        return studentRepository.existsByCollegeIdAndUserId(collegeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID studentIdOf(String userId) {
        UUID collegeId = tenantGuard.currentCollegeId();
        if (userId == null || collegeId == null) {
            return null;
        }

        return studentRepository.findByCollegeIdAndUserId(collegeId, userId)
                .map(Student::getId)
                .orElse(null);
    }
}
