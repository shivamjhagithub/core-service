package com.CoreService.CoreService.syllabus.dto;

/**
 * Aggregate projection: progress is counted in the database so a syllabus with
 * hundreds of topics is never materialised to compute a percentage.
 */
public record SyllabusProgressRow(Long totalTopics, Long completedTopics) {
}
