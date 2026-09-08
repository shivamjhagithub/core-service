package com.CoreService.CoreService.assignment.service;

import com.CoreService.CoreService.academic.service.AcademicLookupService;
import com.CoreService.CoreService.assignment.dto.AssignmentCreateRequest;
import com.CoreService.CoreService.assignment.dto.AssignmentDetailResponse;
import com.CoreService.CoreService.assignment.dto.AssignmentResponse;
import com.CoreService.CoreService.assignment.dto.AssignmentUpdateRequest;
import com.CoreService.CoreService.assignment.dto.AttachmentResponse;
import com.CoreService.CoreService.assignment.dto.SubmissionResponse;
import com.CoreService.CoreService.assignment.entity.Assignment;
import com.CoreService.CoreService.assignment.entity.AssignmentAttachment;
import com.CoreService.CoreService.assignment.entity.AssignmentSubmission;
import com.CoreService.CoreService.assignment.enums.AssignmentStatus;
import com.CoreService.CoreService.assignment.event.AssignmentPublishedEvent;
import com.CoreService.CoreService.assignment.mapper.AssignmentMapper;
import com.CoreService.CoreService.assignment.repository.AssignmentAttachmentRepository;
import com.CoreService.CoreService.assignment.repository.AssignmentRepository;
import com.CoreService.CoreService.assignment.repository.AssignmentSpecifications;
import com.CoreService.CoreService.assignment.repository.AssignmentSubmissionRepository;
import com.CoreService.CoreService.assignment.repository.SubmissionAttachmentRepository;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.exception.BusinessException;
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
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Teacher-side assignment lifecycle and the read model both roles share.
 * Submissions are handled by {@link AssignmentSubmissionService}.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private static final String ASSIGNMENT = "Assignment";
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<AssignmentStatus> STUDENT_VISIBLE_STATUSES =
            EnumSet.of(AssignmentStatus.PUBLISHED, AssignmentStatus.CLOSED);

    private final AssignmentRepository assignmentRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final AssignmentMapper assignmentMapper;
    private final AttachmentResolver attachmentResolver;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;
    private final ClassroomAccessService classroomAccessService;
    private final AcademicLookupService academicLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('ASSIGNMENT_CREATE')")
    public AssignmentResponse create(AssignmentCreateRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        classroomAccessService.requireClassroom(request.classroomId());
        classroomAccessService.assertTeacher(request.classroomId());
        if (request.subjectId() != null) {
            academicLookupService.requireSubject(request.subjectId());
        }
        List<StoredFileRef> attachments = attachmentResolver.requireOwnedFiles(request.attachmentFileIds());

        Assignment assignment = new Assignment();
        assignment.setCollegeId(collegeId);
        assignment.setClassroomId(request.classroomId());
        assignment.setSubjectId(request.subjectId());
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setCreatedBy(tenantGuard.requireUserId());
        assignment.setDueAt(request.dueAt());
        assignment.setMaxMarks(request.maxMarks());
        assignment.setStatus(AssignmentStatus.DRAFT);

        Assignment saved = assignmentRepository.save(assignment);
        addAttachments(collegeId, saved, attachments);

        return assignmentMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    public AssignmentResponse update(UUID assignmentId, AssignmentUpdateRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        Assignment assignment = requireAssignment(collegeId, assignmentId);
        classroomAccessService.assertTeacher(assignment.getClassroomId());

        if (assignment.getStatus() == AssignmentStatus.CLOSED) {
            throw new BusinessException("A closed assignment can no longer be edited");
        }
        if (request.subjectId() != null) {
            academicLookupService.requireSubject(request.subjectId());
        }

        assignment.setSubjectId(request.subjectId());
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueAt(request.dueAt());
        assignment.setMaxMarks(request.maxMarks());
        Assignment saved = assignmentRepository.save(assignment);

        if (request.attachmentFileIds() != null) {
            List<StoredFileRef> attachments = attachmentResolver.requireOwnedFiles(request.attachmentFileIds());
            assignmentAttachmentRepository.deleteByAssignment(collegeId, assignmentId);
            addAttachments(collegeId, saved, attachments);
        }

        return assignmentMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    public AssignmentResponse publish(UUID assignmentId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        Assignment assignment = requireAssignment(collegeId, assignmentId);
        classroomAccessService.assertTeacher(assignment.getClassroomId());

        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new BusinessException("Only a draft assignment can be published");
        }

        Instant now = Instant.now();
        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setPublishedAt(now);
        Assignment saved = assignmentRepository.save(assignment);

        eventPublisher.publishEvent(new AssignmentPublishedEvent(
                collegeId,
                saved.getId(),
                saved.getClassroomId(),
                saved.getTitle(),
                saved.getDueAt(),
                now));

        return assignmentMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    public AssignmentResponse close(UUID assignmentId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        Assignment assignment = requireAssignment(collegeId, assignmentId);
        classroomAccessService.assertTeacher(assignment.getClassroomId());

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("Only a published assignment can be closed");
        }

        assignment.setStatus(AssignmentStatus.CLOSED);
        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ASSIGNMENT_DELETE')")
    public void delete(UUID assignmentId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        Assignment assignment = requireAssignment(collegeId, assignmentId);
        classroomAccessService.assertTeacher(assignment.getClassroomId());

        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new BusinessException("Only a draft assignment can be deleted");
        }

        assignmentAttachmentRepository.deleteByAssignment(collegeId, assignmentId);
        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public PageResponse<AssignmentResponse> listForClassroom(UUID classroomId,
                                                             AssignmentStatus status,
                                                             int page,
                                                             int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        classroomAccessService.requireClassroom(classroomId);

        boolean teacherView = hasTeacherView(classroomId);
        if (!teacherView) {
            classroomAccessService.assertMember(classroomId);
        }

        Page<Assignment> result = assignmentRepository.findAll(
                AssignmentSpecifications.filter(collegeId, List.of(classroomId), visibleStatuses(status, teacherView)),
                pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.from(result, assignmentMapper::toResponse);
    }

    /**
     * Published work across every classroom the caller belongs to; the student
     * view of "what do I have to do".
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public PageResponse<AssignmentResponse> listMine(int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        List<UUID> classroomIds = classroomAccessService.classroomIdsOfUser(tenantGuard.requireUserId());
        PageRequest pageRequest = pageRequest(page, size, Sort.by(Sort.Direction.ASC, "dueAt"));

        Page<Assignment> result = classroomIds.isEmpty()
                ? Page.<Assignment>empty(pageRequest)
                : assignmentRepository.findAll(
                        AssignmentSpecifications.filter(collegeId, classroomIds, STUDENT_VISIBLE_STATUSES),
                        pageRequest);

        return PageResponse.from(result, assignmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public AssignmentDetailResponse detail(UUID assignmentId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ASSIGNMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        Assignment assignment = requireAssignment(collegeId, assignmentId);

        boolean teacherView = hasTeacherView(assignment.getClassroomId());
        if (!teacherView) {
            classroomAccessService.assertMember(assignment.getClassroomId());
            if (assignment.getStatus() == AssignmentStatus.DRAFT) {
                // Drafts must not even be discoverable by students.
                throw new ResourceNotFoundException(ASSIGNMENT, assignmentId);
            }
        }

        List<AttachmentResponse> attachments = attachmentResolver
                .groupByOwner(assignmentAttachmentRepository.findRowsByAssignmentIds(collegeId, List.of(assignmentId)))
                .getOrDefault(assignmentId, List.of());

        String subjectName = assignment.getSubjectId() == null
                ? null
                : academicLookupService.subjectName(assignment.getSubjectId());

        Long submissionCount = null;
        SubmissionResponse mySubmission = null;
        if (teacherView) {
            submissionCount = assignmentSubmissionRepository.countByCollegeIdAndAssignment_Id(collegeId, assignmentId);
        } else {
            mySubmission = assignmentSubmissionRepository
                    .findByCollegeIdAndAssignment_IdAndStudentUserId(collegeId, assignmentId, tenantGuard.requireUserId())
                    .map(submission -> assignmentMapper.toSubmissionResponse(
                            submission,
                            assignmentId,
                            assignment.getTitle(),
                            ownAttachments(collegeId, submission)))
                    .orElse(null);
        }

        return assignmentMapper.toDetailResponse(assignment, subjectName, attachments, submissionCount, mySubmission);
    }

    private List<AttachmentResponse> ownAttachments(UUID collegeId, AssignmentSubmission submission) {
        return attachmentResolver
                .groupByOwner(submissionAttachmentRepository
                        .findRowsBySubmissionIds(collegeId, List.of(submission.getId())))
                .getOrDefault(submission.getId(), List.of());
    }

    private void addAttachments(UUID collegeId, Assignment assignment, List<StoredFileRef> refs) {
        if (refs.isEmpty()) {
            return;
        }
        List<AssignmentAttachment> attachments = refs.stream().map(ref -> {
            AssignmentAttachment attachment = new AssignmentAttachment();
            attachment.setCollegeId(collegeId);
            attachment.setAssignment(assignment);
            attachment.setFileId(ref.fileId());
            return attachment;
        }).toList();
        assignmentAttachmentRepository.saveAll(attachments);
    }

    private Collection<AssignmentStatus> visibleStatuses(AssignmentStatus requested, boolean teacherView) {
        if (teacherView) {
            return requested == null ? null : List.of(requested);
        }
        if (requested == null) {
            return STUDENT_VISIBLE_STATUSES;
        }
        if (!STUDENT_VISIBLE_STATUSES.contains(requested)) {
            throw new ForbiddenException("Draft assignments are only visible to the classroom's teachers");
        }
        return List.of(requested);
    }

    private boolean hasTeacherView(UUID classroomId) {
        return tenantGuard.isMainAdmin()
                || classroomAccessService.isTeacher(classroomId, tenantGuard.requireUserId());
    }

    private Assignment requireAssignment(UUID collegeId, UUID assignmentId) {
        return assignmentRepository.findByIdAndCollegeId(assignmentId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT, assignmentId));
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }
}
