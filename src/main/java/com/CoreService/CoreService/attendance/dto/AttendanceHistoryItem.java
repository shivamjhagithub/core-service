package com.CoreService.CoreService.attendance.dto;

import com.CoreService.CoreService.attendance.enums.AttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceHistoryItem(UUID sessionId,
                                    UUID classroomId,
                                    UUID subjectId,
                                    String subjectName,
                                    LocalDate sessionDate,
                                    AttendanceStatus status,
                                    String remark) {
}
