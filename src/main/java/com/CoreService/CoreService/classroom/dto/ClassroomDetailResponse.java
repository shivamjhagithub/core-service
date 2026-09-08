package com.CoreService.CoreService.classroom.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClassroomDetailResponse(UUID id,
                                      String name,
                                      String description,
                                      String createdBy,
                                      boolean active,
                                      boolean archived,
                                      LocalDateTime createdAt,
                                      LocalDateTime updatedAt,
                                      List<ClassroomMemberResponse> members,
                                      List<ClassroomPermissionResponse> permissions) {
}
