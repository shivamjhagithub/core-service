package com.CoreService.CoreService.academic.controller;

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
import com.CoreService.CoreService.academic.service.AcademicService;
import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic")
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academicService;

    // ------------------------------------------------------------------
    // Departments
    // ------------------------------------------------------------------

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Department created", academicService.createDepartment(request)));
    }

    @PutMapping("/departments/{departmentId}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable UUID departmentId,
            @Valid @RequestBody DepartmentRequest request) {

        return ApiResponse.success("Department updated", academicService.updateDepartment(departmentId, request));
    }

    @DeleteMapping("/departments/{departmentId}")
    public ApiResponse<Void> deleteDepartment(@PathVariable UUID departmentId) {
        academicService.deleteDepartment(departmentId);
        return ApiResponse.message("Department deleted");
    }

    @GetMapping("/departments/{departmentId}")
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable UUID departmentId) {
        return ApiResponse.success(academicService.getDepartment(departmentId));
    }

    @GetMapping("/departments")
    public ApiResponse<PageResponse<DepartmentResponse>> listDepartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(academicService.listDepartments(page, size));
    }

    // ------------------------------------------------------------------
    // Programs
    // ------------------------------------------------------------------

    @PostMapping("/programs")
    public ResponseEntity<ApiResponse<ProgramResponse>> createProgram(
            @Valid @RequestBody ProgramRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Program created", academicService.createProgram(request)));
    }

    @PutMapping("/programs/{programId}")
    public ApiResponse<ProgramResponse> updateProgram(
            @PathVariable UUID programId,
            @Valid @RequestBody ProgramRequest request) {

        return ApiResponse.success("Program updated", academicService.updateProgram(programId, request));
    }

    @DeleteMapping("/programs/{programId}")
    public ApiResponse<Void> deleteProgram(@PathVariable UUID programId) {
        academicService.deleteProgram(programId);
        return ApiResponse.message("Program deleted");
    }

    @GetMapping("/programs/{programId}")
    public ApiResponse<ProgramResponse> getProgram(@PathVariable UUID programId) {
        return ApiResponse.success(academicService.getProgram(programId));
    }

    @GetMapping("/programs")
    public ApiResponse<PageResponse<ProgramResponse>> listPrograms(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(academicService.listPrograms(departmentId, page, size));
    }

    // ------------------------------------------------------------------
    // Academic sessions
    // ------------------------------------------------------------------

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<AcademicSessionResponse>> createSession(
            @Valid @RequestBody AcademicSessionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Academic session created", academicService.createSession(request)));
    }

    @PutMapping("/sessions/{sessionId}")
    public ApiResponse<AcademicSessionResponse> updateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AcademicSessionRequest request) {

        return ApiResponse.success("Academic session updated", academicService.updateSession(sessionId, request));
    }

    @PatchMapping("/sessions/{sessionId}/current")
    public ApiResponse<AcademicSessionResponse> markSessionAsCurrent(@PathVariable UUID sessionId) {
        return ApiResponse.success("Current academic session updated",
                academicService.markSessionAsCurrent(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable UUID sessionId) {
        academicService.deleteSession(sessionId);
        return ApiResponse.message("Academic session deleted");
    }

    @GetMapping("/sessions/current")
    public ApiResponse<AcademicSessionResponse> getCurrentSession() {
        return ApiResponse.success(academicService.getCurrentSession());
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<AcademicSessionResponse> getSession(@PathVariable UUID sessionId) {
        return ApiResponse.success(academicService.getSession(sessionId));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResponse<AcademicSessionResponse>> listSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(academicService.listSessions(page, size));
    }

    // ------------------------------------------------------------------
    // Semesters
    // ------------------------------------------------------------------

    @PostMapping("/semesters")
    public ResponseEntity<ApiResponse<SemesterResponse>> createSemester(
            @Valid @RequestBody SemesterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Semester created", academicService.createSemester(request)));
    }

    @PutMapping("/semesters/{semesterId}")
    public ApiResponse<SemesterResponse> updateSemester(
            @PathVariable UUID semesterId,
            @Valid @RequestBody SemesterRequest request) {

        return ApiResponse.success("Semester updated", academicService.updateSemester(semesterId, request));
    }

    @DeleteMapping("/semesters/{semesterId}")
    public ApiResponse<Void> deleteSemester(@PathVariable UUID semesterId) {
        academicService.deleteSemester(semesterId);
        return ApiResponse.message("Semester deleted");
    }

    @GetMapping("/semesters/{semesterId}")
    public ApiResponse<SemesterResponse> getSemester(@PathVariable UUID semesterId) {
        return ApiResponse.success(academicService.getSemester(semesterId));
    }

    @GetMapping("/semesters")
    public ApiResponse<PageResponse<SemesterResponse>> listSemesters(
            @RequestParam(required = false) UUID programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(academicService.listSemesters(programId, page, size));
    }

    // ------------------------------------------------------------------
    // Subjects
    // ------------------------------------------------------------------

    @PostMapping("/subjects")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @Valid @RequestBody SubjectRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject created", academicService.createSubject(request)));
    }

    @PutMapping("/subjects/{subjectId}")
    public ApiResponse<SubjectResponse> updateSubject(
            @PathVariable UUID subjectId,
            @Valid @RequestBody SubjectRequest request) {

        return ApiResponse.success("Subject updated", academicService.updateSubject(subjectId, request));
    }

    @DeleteMapping("/subjects/{subjectId}")
    public ApiResponse<Void> deleteSubject(@PathVariable UUID subjectId) {
        academicService.deleteSubject(subjectId);
        return ApiResponse.message("Subject deleted");
    }

    @GetMapping("/subjects/{subjectId}")
    public ApiResponse<SubjectResponse> getSubject(@PathVariable UUID subjectId) {
        return ApiResponse.success(academicService.getSubject(subjectId));
    }

    @GetMapping("/subjects")
    public ApiResponse<PageResponse<SubjectResponse>> listSubjects(
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(academicService.listSubjects(semesterId, page, size));
    }
}
