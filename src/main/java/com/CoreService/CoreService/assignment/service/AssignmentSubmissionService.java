package com.CoreService.CoreService.assignment.service;

import com.CoreService.CoreService.assignment.dto.AttachmentResponse;
import com.CoreService.CoreService.assignment.dto.GradeRequest;
import com.CoreService.CoreService.assignment.dto.SubmissionRequest;
import com.CoreService.CoreService.assignment.dto.SubmissionResponse;
import com.CoreService.CoreService.assignment.entity.Assignment;
import com.CoreService.CoreService.assignment.entity.AssignmentSubmission;
import com.CoreService.CoreService.assignment.entity.SubmissionAttachment;
import com.CoreService.CoreService.assignment.enums.AssignmentStatus;
import com.CoreService.CoreService.assignment.enums.SubmissionStatus;
import com.CoreService.CoreService.assignment.event.AssignmentGradedEvent;
import com.CoreService.CoreService.assignment.event.AssignmentSubmittedEvent;
import com.CoreService.CoreService.assignment.mapper.AssignmentMapper;
import com.CoreService.CoreService.assignment.repository.AssignmentRepository;
import com.CoreService.CoreService.assignment.repository.AssignmentSubmissionRepository;
import com.CoreService.CoreService.assignment.repository.SubmissionAttachmentRepository;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.DuplicateResourceException;
import com.CoreService.CoreService.common.exception.ForbiddenException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.file.dto.StoredFileRef;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Student submissions and teacher grading. A student may only ever reach their
 * own submission; the checks live here rather than in the controller so every
 * caller is covered.
 */
@Service
@RequiredArgsConstructor
public class AssignmentSubmissionService {

    private static final String ASSIGNMENT = "Assignment";
    private static final String SUBMISSION = "Submission";
    private static final int MAX_PAGE_SIZE = 100;

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final AssignmentMapper assignmentMapper;
    private final AttachmentResolver attachmentResolver;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;
    private final ClassroomAccessService classroomAccessService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('ASSIGNMENT_SUBMIT')")
    public SubmissionResponse submit(UUID assignmentId, SubmissionRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        String studentUserId = tenantGuard.requireUserId();

        Assignment assignment = assignmentRepository.findByIdAndCollegeId(assignmentId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT, assignmentId));
        classroomAccessService.assertMember(assignment.getClassroomId());

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("Submissions are only accepted while the assignment is published");
        }
        if (assignmentSubmissionRepository
                .existsByCollegeIdAndAssignment_IdAndStudentUserId(collegeId, assignmentId, studentUserId)) {
            throw new DuplicateResourceException("You have already submitted this assignment");
        }

        List<StoredFileRef> refs = attachmentResolver.requireOwnedFiles(request.attachmentFileIds());
        Instant now = Instant.now();
        boolean late = assignment.getDueAt() != null && now.isAfter(assignment.getDueAt());

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setCollegeId(collegeId);
        submission.setAssignment(assignment);
        submission.setStudentUserId(studentUserId);
        submission.setContent(request.content());
        submission.setSubmittedAt(now);
        submission.setStatus(late ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED);

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
        addAttachments(collegeId, saved, refs);

        eventPublisher.publishEvent(new AssignmentSubmittedEvent(
                collegeId,
                assignmentId,
                saved.getId(),
                assignment.getClassroomId(),
                studentUserId,
                late,
                now));

        return assignmentMapper.toSubmissionResponse(saved, assignmentId, assignment.getTitle(),
                attachmentResolver.toResponses(refs));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ASSIGNMENT_SUBMIT')")
    public SubmissionResponse updateMySubmission(UUID submissionId, SubmissionRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        AssignmentSubmission submission = requireSubmission(collegeId, submissionId);
        Assignment assignment = submission.getAssignment();

        if (!submission.getStudentUserId().equals(tenantGuard.requireUserId())) {
            throw new ForbiddenException("You can only change your own submission");
        }
        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new BusinessException("A graded submission can no longer be changed");
        }
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("Submissions are only accepted while the assignment is published");
        }

        Instant now = Instant.now();
        if (assignment.getDueAt() != null && now.isAfter(assignment.getDueAt())) {
            throw new BusinessException("The due date has passed, this submission can no longer be changed");
        }

        submission.setContent(request.content());
        submission.setSubmittedAt(now);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);

        List<AttachmentResponse> attachments;
        if (request.attachmentFileIds() == null) {
            attachments = attachmentsOf(collegeId, submissionId);
        } else {
            List<StoredFileRef> refs = attachmentResolver.requireOwnedFiles(request.attachmentFileIds());
            submissionAttachmentRepository.deleteBySubmission(collegeId, submissionId);
            addAttachments(collegeId, saved, refs);
            attachments = attachmentResolver.toResponses(refs);
        }

        return assignmentMapper.toSubmissionResponse(saved, assignment.getId(), assignment.getTitle(), attachments);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ASSIGNMENT_GRADE')")
    public SubmissionResponse grade(UUID submissionId, GradeRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        AssignmentSubmission submission = requireSubmission(collegeId, submissionId);
        Assignment assignment = submission.getAssignment();
        classroomAccessService.assertTeacher(assignment.getClassroomId());

        if (submission.getSubmittedAt() == null) {
            throw new BusinessException("This assignment has not been submitted yet");
        }
        if (request.marks() == null) {
            throw new BusinessException("Marks are required");
        }
        if (request.marks() < 0d) {
            throw new BusinessException("Marks must not be negative");
        }
        if (assignment.getMaxMarks() != null && request.marks() > assignment.getMaxMarks()) {
            throw new BusinessException("Marks must not exceed the maximum of " + assignment.getMaxMarks());
        }

        Instant now = Instant.now();
        submission.setMarks(request.marks());
        submission.setFeedback(request.feedback());
        submission.setGradedBy(tenantGuard.requireUserId());
        submission.setGradedAt(now);
        submission.setStatus(SubmissionStatus.GRADED);
        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);

        eventPublisher.publishEvent(new AssignmentGradedEvent(
                collegeId,
                assignment.getId(),
                saved.getId(),
                saved.getStudentUserId(),
                assignment.getTitle(),
                saved.getMarks(),
                now));

        return assignmentMapper.toSubmissionResponse(saved, assignment.getId(), assignment.getTitle(),
                attachmentsOf(collegeId, submissionId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public PageResponse<SubmissionResponse> listForAssignment(UUID assignmentId, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        Assignment assignment = assignmentRepository.findByIdAndCollegeId(assignmentId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT, assignmentId));
        classroomAccessService.assertTeacher(assignment.getClassroomId());

        Page<AssignmentSubmission> result = assignmentSubmissionRepository.findByCollegeIdAndAssignment_Id(
                collegeId, assignmentId, pageRequest(page, size, Sort.by(Sort.Direction.DESC, "submittedAt")));

        Map<UUID, List<AttachmentResponse>> attachments = attachmentsBySubmission(collegeId,
                result.getContent().stream().map(AssignmentSubmission::getId).toList());

        return PageResponse.from(result, submission -> assignmentMapper.toSubmissionResponse(
                submission,
                assignmentId,
                assignment.getTitle(),
                attachments.getOrDefault(submission.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public PageResponse<SubmissionResponse> listMine(int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        Page<AssignmentSubmission> result = assignmentSubmissionRepository.findByStudent(
                collegeId,
                tenantGuard.requireUserId(),
                pageRequest(page, size, Sort.by(Sort.Direction.DESC, "submittedAt")));

        Map<UUID, List<AttachmentResponse>> attachments = attachmentsBySubmission(collegeId,
                result.getContent().stream().map(AssignmentSubmission::getId).toList());

        return PageResponse.from(result, submission -> assignmentMapper.toSubmissionResponse(
                submission,
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                attachments.getOrDefault(submission.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public SubmissionResponse detail(UUID submissionId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        AssignmentSubmission submission = requireSubmission(collegeId, submissionId);
        Assignment assignment = submission.getAssignment();

        String userId = tenantGuard.requireUserId();
        boolean owner = submission.getStudentUserId().equals(userId);
        if (!owner && !tenantGuard.isMainAdmin()
                && !classroomAccessService.isTeacher(assignment.getClassroomId(), userId)) {
            throw new ForbiddenException("Only the student and the classroom's teachers may view this submission");
        }

        return assignmentMapper.toSubmissionResponse(submission, assignment.getId(), assignment.getTitle(),
                attachmentsOf(collegeId, submissionId));
    }

    private void addAttachments(UUID collegeId, AssignmentSubmission submission, List<StoredFileRef> refs) {
        if (refs.isEmpty()) {
            return;
        }
        List<SubmissionAttachment> attachments = refs.stream().map(ref -> {
            SubmissionAttachment attachment = new SubmissionAttachment();
            attachment.setCollegeId(collegeId);
            attachment.setSubmission(submission);
            attachment.setFileId(ref.fileId());
            return attachment;
        }).toList();
        submissionAttachmentRepository.saveAll(attachments);
    }

    private List<AttachmentResponse> attachmentsOf(UUID collegeId, UUID submissionId) {
        return attachmentsBySubmission(collegeId, List.of(submissionId)).getOrDefault(submissionId, List.of());
    }

    private Map<UUID, List<AttachmentResponse>> attachmentsBySubmission(UUID collegeId, List<UUID> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        return attachmentResolver.groupByOwner(
                submissionAttachmentRepository.findRowsBySubmissionIds(collegeId, submissionIds));
    }

    private AssignmentSubmission requireSubmission(UUID collegeId, UUID submissionId) {
        return assignmentSubmissionRepository.findWithAssignmentByIdAndCollegeId(submissionId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBMISSION, submissionId));
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }
}
