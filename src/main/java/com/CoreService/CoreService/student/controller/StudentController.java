package com.CoreService.CoreService.student.controller;

import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.student.dto.StudentRequest;
import com.CoreService.CoreService.student.dto.StudentResponse;
import com.CoreService.CoreService.student.dto.StudentUpdateRequest;
import com.CoreService.CoreService.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody StudentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student created", studentService.createStudent(request)));
    }

    @PutMapping("/{studentId}")
    public ApiResponse<StudentResponse> updateStudent(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentUpdateRequest request) {

        return ApiResponse.success("Student updated", studentService.updateStudent(studentId, request));
    }

    @PatchMapping("/{studentId}/activate")
    public ApiResponse<StudentResponse> activateStudent(@PathVariable UUID studentId) {
        return ApiResponse.success("Student activated", studentService.activateStudent(studentId));
    }

    @PatchMapping("/{studentId}/deactivate")
    public ApiResponse<StudentResponse> deactivateStudent(@PathVariable UUID studentId) {
        return ApiResponse.success("Student deactivated", studentService.deactivateStudent(studentId));
    }

    @GetMapping("/me")
    public ApiResponse<StudentResponse> getMyProfile() {
        return ApiResponse.success(studentService.getMyProfile());
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<StudentResponse>> searchStudents(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(studentService.searchStudents(keyword, page, size));
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<StudentResponse> getStudentByUserId(@PathVariable String userId) {
        return ApiResponse.success(studentService.getStudentByUserId(userId));
    }

    @GetMapping("/{studentId}")
    public ApiResponse<StudentResponse> getStudent(@PathVariable UUID studentId) {
        return ApiResponse.success(studentService.getStudent(studentId));
    }

    @GetMapping
    public ApiResponse<PageResponse<StudentResponse>> listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(studentService.listStudents(page, size));
    }
}
