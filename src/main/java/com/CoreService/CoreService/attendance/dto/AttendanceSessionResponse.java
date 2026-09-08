package com.CoreService.CoreService.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceSessionResponse(UUID id,
                                        UUID classroomId,
                                        UUID subjectId,
                                        String subjectName,
                                        String teacherUserId,
                                        LocalDate sessionDate,
                                        String remark,
                                        boolean locked,
                                        AttendanceTotals totals,
                                        LocalDateTime createdAt) {
}
