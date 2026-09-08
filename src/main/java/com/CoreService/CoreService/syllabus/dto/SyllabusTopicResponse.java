package com.CoreService.CoreService.syllabus.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SyllabusTopicResponse(UUID id,
                                    UUID unitId,
                                    String title,
                                    String description,
                                    Integer orderIndex,
                                    boolean completed,
                                    LocalDateTime completedAt,
                                    String completedBy) {
}
