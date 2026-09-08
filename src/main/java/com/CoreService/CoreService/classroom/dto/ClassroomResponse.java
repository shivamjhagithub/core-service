package com.CoreService.CoreService.classroom.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClassroomResponse(UUID id,
                                String name,
                                String description,
                                String createdBy,
                                boolean active,
                                boolean archived,
                                long memberCount,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
}
