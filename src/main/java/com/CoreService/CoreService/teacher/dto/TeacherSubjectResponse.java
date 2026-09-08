package com.CoreService.CoreService.teacher.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TeacherSubjectResponse(
        UUID id,
        UUID teacherId,
        UUID subjectId,
        LocalDateTime createdAt) {
}
