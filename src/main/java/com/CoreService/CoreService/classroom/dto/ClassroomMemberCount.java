package com.CoreService.CoreService.classroom.dto;

import java.util.UUID;

/**
 * Aggregate row used to attach member counts to a page of classrooms with a
 * single query instead of one count per row.
 */
public record ClassroomMemberCount(UUID classroomId, Long memberCount) {
}
