package com.CoreService.CoreService.assignment.controller;

import com.CoreService.CoreService.assignment.dto.AssignmentCreateRequest;
import com.CoreService.CoreService.assignment.dto.AssignmentDetailResponse;
import com.CoreService.CoreService.assignment.dto.AssignmentResponse;
import com.CoreService.CoreService.assignment.dto.AssignmentUpdateRequest;
import com.CoreService.CoreService.assignment.dto.GradeRequest;
import com.CoreService.CoreService.assignment.dto.SubmissionRequest;
import com.CoreService.CoreService.assignment.dto.SubmissionResponse;
import com.CoreService.CoreService.assignment.enums.AssignmentStatus;
import com.CoreService.CoreService.assignment.service.AssignmentService;
import com.CoreService.CoreService.assignment.service.AssignmentSubmissionService;
import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentSubmissionService assignmentSubmissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> create(
            @Valid @RequestBody AssignmentCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment created", assignmentService.create(request)));
    }

    @PutMapping("/{assignmentId}")
    public ApiResponse<AssignmentResponse> update(@PathVariable UUID assignmentId,
                                                  @Valid @RequestBody AssignmentUpdateRequest request) {
        return ApiResponse.success("Assignment updated", assignmentService.update(assignmentId, request));
    }

    @PostMapping("/{assignmentId}/publish")
    public ApiResponse<AssignmentResponse> publish(@PathVariable UUID assignmentId) {
        return ApiResponse.success("Assignment published", assignmentService.publish(assignmentId));
    }

    @PostMapping("/{assignmentId}/close")
    public ApiResponse<AssignmentResponse> close(@PathVariable UUID assignmentId) {
        return ApiResponse.success("Assignment closed", assignmentService.close(assignmentId));
    }

    @DeleteMapping("/{assignmentId}")
    public ApiResponse<Void> delete(@PathVariable UUID assignmentId) {
        assignmentService.delete(assignmentId);
        return ApiResponse.message("Assignment deleted");
    }

    @GetMapping
    public ApiResponse<PageResponse<AssignmentResponse>> listForClassroom(
            @RequestParam UUID classroomId,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(assignmentService.listForClassroom(classroomId, status, page, size));
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<AssignmentResponse>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(assignmentService.listMine(page, size));
    }

    @GetMapping("/{assignmentId}")
    public ApiResponse<AssignmentDetailResponse> detail(@PathVariable UUID assignmentId) {
        return ApiResponse.success(assignmentService.detail(assignmentId));
    }

    @PostMapping("/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody SubmissionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment submitted",
                        assignmentSubmissionService.submit(assignmentId, request)));
    }

    @GetMapping("/{assignmentId}/submissions")
    public ApiResponse<PageResponse<SubmissionResponse>> listSubmissions(
            @PathVariable UUID assignmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(assignmentSubmissionService.listForAssignment(assignmentId, page, size));
    }

    @GetMapping("/submissions/my")
    public ApiResponse<PageResponse<SubmissionResponse>> listMySubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(assignmentSubmissionService.listMine(page, size));
    }

    @GetMapping("/submissions/{submissionId}")
    public ApiResponse<SubmissionResponse> submissionDetail(@PathVariable UUID submissionId) {
        return ApiResponse.success(assignmentSubmissionService.detail(submissionId));
    }

    @PutMapping("/submissions/{submissionId}")
    public ApiResponse<SubmissionResponse> updateSubmission(@PathVariable UUID submissionId,
                                                            @Valid @RequestBody SubmissionRequest request) {
        return ApiResponse.success("Submission updated",
                assignmentSubmissionService.updateMySubmission(submissionId, request));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public ApiResponse<SubmissionResponse> grade(@PathVariable UUID submissionId,
                                                 @Valid @RequestBody GradeRequest request) {
        return ApiResponse.success("Submission graded",
                assignmentSubmissionService.grade(submissionId, request));
    }
}
