package com.CoreService.CoreService.attendance.mapper;

import com.CoreService.CoreService.attendance.dto.AttendanceHistoryItem;
import com.CoreService.CoreService.attendance.dto.AttendanceRecordResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceSessionDetailResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceSessionResponse;
import com.CoreService.CoreService.attendance.dto.AttendanceTotals;
import com.CoreService.CoreService.attendance.entity.AttendanceRecord;
import com.CoreService.CoreService.attendance.entity.AttendanceSession;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AttendanceMapper {

    public AttendanceSessionResponse toSessionResponse(AttendanceSession session,
                                                       String subjectName,
                                                       AttendanceTotals totals) {
        return new AttendanceSessionResponse(session.getId(),
                session.getClassroomId(),
                session.getSubjectId(),
                subjectName,
                session.getTeacherUserId(),
                session.getSessionDate(),
                session.getRemark(),
                session.isLocked(),
                totals,
                session.getCreatedAt());
    }

    public AttendanceSessionDetailResponse toSessionDetailResponse(AttendanceSession session,
                                                                   String subjectName,
                                                                   AttendanceTotals totals,
                                                                   List<AttendanceRecordResponse> records) {
        return new AttendanceSessionDetailResponse(session.getId(),
                session.getClassroomId(),
                session.getSubjectId(),
                subjectName,
                session.getTeacherUserId(),
                session.getSessionDate(),
                session.getRemark(),
                session.isLocked(),
                totals,
                records,
                session.getCreatedAt());
    }

    public AttendanceRecordResponse toRecordResponse(AttendanceRecord attendanceRecord, String studentName) {
        return new AttendanceRecordResponse(attendanceRecord.getId(),
                attendanceRecord.getStudentUserId(),
                studentName,
                attendanceRecord.getStatus(),
                attendanceRecord.getRemark());
    }

    public List<AttendanceRecordResponse> toRecordResponses(List<AttendanceRecord> attendanceRecords,
                                                            Map<String, String> studentNames) {
        return attendanceRecords.stream()
                .map(attendanceRecord -> toRecordResponse(attendanceRecord,
                        studentNames.get(attendanceRecord.getStudentUserId())))
                .toList();
    }

    public AttendanceHistoryItem toHistoryItem(AttendanceRecord attendanceRecord, String subjectName) {
        AttendanceSession session = attendanceRecord.getSession();
        return new AttendanceHistoryItem(session.getId(),
                session.getClassroomId(),
                session.getSubjectId(),
                subjectName,
                session.getSessionDate(),
                attendanceRecord.getStatus(),
                attendanceRecord.getRemark());
    }
}
