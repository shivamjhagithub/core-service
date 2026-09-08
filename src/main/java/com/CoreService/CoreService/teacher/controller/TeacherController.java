package com.CoreService.CoreService.teacher.controller;

import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.teacher.dto.TeacherRequest;
import com.CoreService.CoreService.teacher.dto.TeacherResponse;
import com.CoreService.CoreService.teacher.dto.TeacherSubjectResponse;
import com.CoreService.CoreService.teacher.dto.TeacherUpdateRequest;
import com.CoreService.CoreService.teacher.service.TeacherService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    public ResponseEntity<ApiResponse<TeacherResponse>> createTeacher(
            @Valid @RequestBody TeacherRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Teacher created", teacherService.createTeacher(request)));
    }

    @PutMapping("/{teacherId}")
    public ApiResponse<TeacherResponse> updateTeacher(
            @PathVariable UUID teacherId,
            @Valid @RequestBody TeacherUpdateRequest request) {

        return ApiResponse.success("Teacher updated", teacherService.updateTeacher(teacherId, request));
    }

    @PatchMapping("/{teacherId}/activate")
    public ApiResponse<TeacherResponse> activateTeacher(@PathVariable UUID teacherId) {
        return ApiResponse.success("Teacher activated", teacherService.activateTeacher(teacherId));
    }

    @PatchMapping("/{teacherId}/deactivate")
    public ApiResponse<TeacherResponse> deactivateTeacher(@PathVariable UUID teacherId) {
        return ApiResponse.success("Teacher deactivated", teacherService.deactivateTeacher(teacherId));
    }

    @GetMapping("/me")
    public ApiResponse<TeacherResponse> getMyProfile() {
        return ApiResponse.success(teacherService.getMyProfile());
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<TeacherResponse>> searchTeachers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(teacherService.searchTeachers(keyword, page, size));
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<TeacherResponse> getTeacherByUserId(@PathVariable String userId) {
        return ApiResponse.success(teacherService.getTeacherByUserId(userId));
    }

    @GetMapping("/{teacherId}")
    public ApiResponse<TeacherResponse> getTeacher(@PathVariable UUID teacherId) {
        return ApiResponse.success(teacherService.getTeacher(teacherId));
    }

    @GetMapping
    public ApiResponse<PageResponse<TeacherResponse>> listTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(teacherService.listTeachers(page, size));
    }

    @PostMapping("/{teacherId}/subjects/{subjectId}")
    public ResponseEntity<ApiResponse<TeacherSubjectResponse>> assignSubject(
            @PathVariable UUID teacherId,
            @PathVariable UUID subjectId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject assigned", teacherService.assignSubject(teacherId, subjectId)));
    }

    @DeleteMapping("/{teacherId}/subjects/{subjectId}")
    public ApiResponse<Void> removeSubject(
            @PathVariable UUID teacherId,
            @PathVariable UUID subjectId) {

        teacherService.removeSubject(teacherId, subjectId);
        return ApiResponse.message("Subject assignment removed");
    }

    @GetMapping("/{teacherId}/subjects")
    public ApiResponse<List<TeacherSubjectResponse>> listAssignedSubjects(@PathVariable UUID teacherId) {
        return ApiResponse.success(teacherService.listAssignedSubjects(teacherId));
    }
}
