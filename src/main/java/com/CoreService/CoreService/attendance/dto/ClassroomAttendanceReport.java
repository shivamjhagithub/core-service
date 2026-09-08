package com.CoreService.CoreService.attendance.dto;

import java.util.List;
import java.util.UUID;

public record ClassroomAttendanceReport(UUID classroomId,
                                        long totalSessions,
                                        List<StudentAttendanceReportRow> students) {
}
