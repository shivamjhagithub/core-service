package com.CoreService.CoreService.classroom.dto;

import java.util.UUID;

/**
 * Minimal classroom projection other modules are allowed to hold. Keeps
 * callers away from the classroom entity and its table.
 */
public record ClassroomRef(UUID classroomId, UUID collegeId, String name, boolean active) {
}
