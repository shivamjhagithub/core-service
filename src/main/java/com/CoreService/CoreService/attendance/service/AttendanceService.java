package com.CoreService.CoreService.attendance.service;

import com.CoreService.CoreService.attendance.dto.AttendancePercentageResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceRecordResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceSessionDetailResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceSessionResponse;
import com.CoreService.CoreService.attendance.dto.ClassroomAttendanceReport;
import com.CoreService.CoreService.attendance.dto.CreateAttendanceSessionRequest;
import com.CoreService.CoreService.attendance.dto.MarkAttendanceRequest;
import com.CoreService.CoreService.attendance.dto.MyAttendanceResponse;
import com.CoreService.CoreService.attendance.dto.UpdateAttendanceRecordRequest;
import com.CoreService.CoreService.common.response.PageResponse;

import java.util.UUID;

public interface AttendanceService {

    AttendanceSessionDetailResponse createSession(CreateAttendanceSessionRequest request);

    AttendanceSessionDetailResponse getSession(UUID sessionId);

    PageResponse<AttendanceSessionResponse> listSessions(UUID classroomId, int page, int size);

    AttendanceSessionDetailResponse markAttendance(UUID sessionId, MarkAttendanceRequest request);

    AttendanceSessionDetailResponse updateAttendance(UUID sessionId, MarkAttendanceRequest request);

    AttendanceRecordResponse updateRecord(UUID recordId, UpdateAttendanceRecordRequest request);

    AttendanceSessionResponse lockSession(UUID sessionId);

    /** Attendance of the calling user; {@code classroomId} may be null for every classroom. */
    MyAttendanceResponse myAttendance(UUID classroomId);

    AttendancePercentageResponse attendancePercentage(UUID classroomId, String studentUserId);

    ClassroomAttendanceReport classroomReport(UUID classroomId);
}
