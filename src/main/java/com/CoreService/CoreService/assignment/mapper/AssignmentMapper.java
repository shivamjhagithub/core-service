package com.CoreService.CoreService.assignment.mapper;

import com.CoreService.CoreService.assignment.dto.AssignmentDetailResponse;
import com.CoreService.CoreService.assignment.dto.AssignmentResponse;
import com.CoreService.CoreService.assignment.dto.AttachmentResponse;
import com.CoreService.CoreService.assignment.dto.SubmissionResponse;
import com.CoreService.CoreService.assignment.entity.Assignment;
import com.CoreService.CoreService.assignment.entity.AssignmentSubmission;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AssignmentMapper {

    public AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getClassroomId(),
                assignment.getSubjectId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getCreatedBy(),
                assignment.getDueAt(),
                assignment.getMaxMarks(),
                assignment.getStatus(),
                assignment.getPublishedAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    public AssignmentDetailResponse toDetailResponse(Assignment assignment,
                                                     String subjectName,
                                                     List<AttachmentResponse> attachments,
                                                     Long submissionCount,
                                                     SubmissionResponse mySubmission) {
        return new AssignmentDetailResponse(
                assignment.getId(),
                assignment.getClassroomId(),
                assignment.getSubjectId(),
                subjectName,
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getCreatedBy(),
                assignment.getDueAt(),
                assignment.getMaxMarks(),
                assignment.getStatus(),
                assignment.getPublishedAt(),
                attachments,
                submissionCount,
                mySubmission,
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    /**
     * The assignment is passed in rather than read off the submission: callers
     * already hold it, and reaching through the association would cost a query
     * per row.
     */
    public SubmissionResponse toSubmissionResponse(AssignmentSubmission submission,
                                                   UUID assignmentId,
                                                   String assignmentTitle,
                                                   List<AttachmentResponse> attachments) {
        return new SubmissionResponse(
                submission.getId(),
                assignmentId,
                assignmentTitle,
                submission.getStudentUserId(),
                submission.getContent(),
                submission.getSubmittedAt(),
                submission.getStatus(),
                submission.getMarks(),
                submission.getFeedback(),
                submission.getGradedBy(),
                submission.getGradedAt(),
                attachments);
    }
}
