package com.CoreService.CoreService.academic.service;

import com.CoreService.CoreService.academic.entity.Subject;
import com.CoreService.CoreService.academic.repository.SubjectRepository;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.security.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Referential-integrity checks other modules run before storing a subject id.
 * <p>
 * Deliberately free of {@code @PreAuthorize} and of the module-enabled gate: it
 * is never a request entry point, only a validation step inside an operation the
 * calling module has already authorized.
 */
@Service
@RequiredArgsConstructor
public class AcademicLookupServiceImpl implements AcademicLookupService {

    private static final String SUBJECT = "Subject";

    private final SubjectRepository subjectRepository;
    private final TenantGuard tenantGuard;

    @Override
    @Transactional(readOnly = true)
    public void requireSubject(UUID subjectId) {
        if (!subjectExists(subjectId)) {
            throw new ResourceNotFoundException(SUBJECT, subjectId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String subjectName(UUID subjectId) {
        if (subjectId == null) {
            throw new ResourceNotFoundException("Subject id must be provided");
        }

        return subjectRepository.findByIdAndCollegeId(subjectId, tenantGuard.requireCollegeId())
                .map(Subject::getName)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT, subjectId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean subjectExists(UUID subjectId) {
        if (subjectId == null) {
            return false;
        }

        UUID collegeId = tenantGuard.currentCollegeId();
        return collegeId != null && subjectRepository.existsByIdAndCollegeId(subjectId, collegeId);
    }
}
