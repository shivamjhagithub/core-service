package com.CoreService.CoreService.attendance.dto;

import java.util.List;
import java.util.UUID;

public record MyAttendanceResponse(String studentUserId,
                                   UUID classroomId,
                                   AttendanceTotals overall,
                                   List<SubjectAttendanceSummary> subjects,
                                   List<AttendanceHistoryItem> history) {
}
