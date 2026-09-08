package com.CoreService.CoreService.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AttendanceSessionDetailResponse(UUID id,
                                              UUID classroomId,
                                              UUID subjectId,
                                              String subjectName,
                                              String teacherUserId,
                                              LocalDate sessionDate,
                                              String remark,
                                              boolean locked,
                                              AttendanceTotals totals,
                                              List<AttendanceRecordResponse> records,
                                              LocalDateTime createdAt) {
}
