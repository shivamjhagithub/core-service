package com.CoreService.CoreService.audit.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(UUID id,
                               String userId,
                               String action,
                               String resourceType,
                               String resourceId,
                               LocalDateTime createdAt) {
}
