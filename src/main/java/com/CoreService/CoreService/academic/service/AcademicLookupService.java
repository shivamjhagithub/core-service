package com.CoreService.CoreService.academic.service;

import java.util.UUID;

/**
 * Academic module's published API for modules that need to reference a subject
 * (attendance sessions, syllabi, study materials) without querying academic
 * tables directly.
 */
public interface AcademicLookupService {

    /**
     * Validates that the subject exists inside the caller's college.
     *
     * @throws com.CoreService.CoreService.common.exception.ResourceNotFoundException
     *         when the subject does not belong to the caller's college
     */
    void requireSubject(UUID subjectId);

    String subjectName(UUID subjectId);

    boolean subjectExists(UUID subjectId);
}
