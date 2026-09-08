package com.CoreService.CoreService.syllabus.dto;

import java.util.List;
import java.util.UUID;

public record SyllabusUnitResponse(UUID id,
                                   UUID syllabusId,
                                   String title,
                                   String description,
                                   Integer orderIndex,
                                   List<SyllabusTopicResponse> topics) {
}
