package com.CoreService.CoreService.syllabus.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SyllabusDetailResponse(UUID id,
                                     String title,
                                     String description,
                                     UUID subjectId,
                                     String subjectName,
                                     UUID classroomId,
                                     boolean published,
                                     SyllabusProgressResponse progress,
                                     List<SyllabusUnitResponse> units,
                                     LocalDateTime createdAt,
                                     LocalDateTime updatedAt) {
}
