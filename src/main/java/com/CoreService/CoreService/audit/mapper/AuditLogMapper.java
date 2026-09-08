package com.CoreService.CoreService.audit.mapper;

import com.CoreService.CoreService.audit.dto.AuditLogResponse;
import com.CoreService.CoreService.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUserId(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getCreatedAt());
    }
}
