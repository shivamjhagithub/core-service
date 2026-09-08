package com.CoreService.CoreService.syllabus.dto;

import java.util.UUID;

public record SyllabusProgressResponse(UUID syllabusId,
                                       long totalTopics,
                                       long completedTopics,
                                       double completionPercentage) {
}
