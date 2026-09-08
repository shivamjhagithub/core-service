package com.CoreService.CoreService.attendance.controller;

import com.CoreService.CoreService.attendance.dto.AttendancePercentageResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceRecordResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceSessionDetailResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceSessionResponse;
import com.CoreService.CoreService.attendance.dto.ClassroomAttendanceReport;
import com.CoreService.CoreService.attendance.dto.CreateAttendanceSessionRequest;
import com.CoreService.CoreService.attendance.dto.MarkAttendanceRequest;
import com.CoreService.CoreService.attendance.dto.MyAttendanceResponse;
import com.CoreService.CoreService.attendance.dto.UpdateAttendanceRecordRequest;
import com.CoreService.CoreService.attendance.service.AttendanceService;
import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/sessions")
    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    public ApiResponse<AttendanceSessionDetailResponse> createSession(
            @Valid @RequestBody CreateAttendanceSessionRequest request) {

        return ApiResponse.success("Attendance session created", attendanceService.createSession(request));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW')")
    public ApiResponse<AttendanceSessionDetailResponse> getSession(@PathVariable UUID sessionId) {
        return ApiResponse.success(attendanceService.getSession(sessionId));
    }

    @GetMapping("/classrooms/{classroomId}/sessions")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW')")
    public ApiResponse<PageResponse<AttendanceSessionResponse>> listSessions(
            @PathVariable UUID classroomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(attendanceService.listSessions(classroomId, page, size));
    }

    @PostMapping("/sessions/{sessionId}/records")
    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    public ApiResponse<AttendanceSessionDetailResponse> markAttendance(
            @PathVariable UUID sessionId,
            @Valid @RequestBody MarkAttendanceRequest request) {

        return ApiResponse.success("Attendance marked", attendanceService.markAttendance(sessionId, request));
    }

    @PutMapping("/sessions/{sessionId}/records")
    @PreAuthorize("hasAuthority('ATTENDANCE_UPDATE')")
    public ApiResponse<AttendanceSessionDetailResponse> updateAttendance(
            @PathVariable UUID sessionId,
            @Valid @RequestBody MarkAttendanceRequest request) {

        return ApiResponse.success("Attendance updated", attendanceService.updateAttendance(sessionId, request));
    }

    @PutMapping("/records/{recordId}")
    @PreAuthorize("hasAuthority('ATTENDANCE_UPDATE')")
    public ApiResponse<AttendanceRecordResponse> updateRecord(
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateAttendanceRecordRequest request) {

        return ApiResponse.success("Attendance record updated", attendanceService.updateRecord(recordId, request));
    }

    @PostMapping("/sessions/{sessionId}/lock")
    @PreAuthorize("hasAuthority('ATTENDANCE_UPDATE')")
    public ApiResponse<AttendanceSessionResponse> lockSession(@PathVariable UUID sessionId) {
        return ApiResponse.success("Attendance session locked", attendanceService.lockSession(sessionId));
    }

    @GetMapping("/me")
    public ApiResponse<MyAttendanceResponse> myAttendance(
            @RequestParam(required = false) UUID classroomId) {

        return ApiResponse.success(attendanceService.myAttendance(classroomId));
    }

    @GetMapping("/classrooms/{classroomId}/students/{studentUserId}/percentage")
    public ApiResponse<AttendancePercentageResponse> attendancePercentage(
            @PathVariable UUID classroomId,
            @PathVariable String studentUserId) {

        return ApiResponse.success(attendanceService.attendancePercentage(classroomId, studentUserId));
    }

    @GetMapping("/classrooms/{classroomId}/report")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW')")
    public ApiResponse<ClassroomAttendanceReport> classroomReport(@PathVariable UUID classroomId) {
        return ApiResponse.success(attendanceService.classroomReport(classroomId));
    }
}
