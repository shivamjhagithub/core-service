package com.CoreService.CoreService.audit.service;

import com.CoreService.CoreService.audit.dto.AuditLogResponse;
import com.CoreService.CoreService.audit.entity.AuditLog;
import com.CoreService.CoreService.audit.mapper.AuditLogMapper;
import com.CoreService.CoreService.audit.repository.AuditLogRepository;
import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Writes and reads the activity trail.
 * <p>
 * The recorder accepts nothing but an action, a resource type and a resource
 * id. There is no free-form payload and no request body is ever copied into the
 * table, so a password, token or other secret cannot reach the trail.
 */
@Service
@RequiredArgsConstructor
public class AuditService implements AuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final int MAX_TEXT_LENGTH = 100;

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final TenantGuard tenantGuard;

    /**
     * REQUIRES_NEW gives the trail its own transaction: the record survives a
     * business transaction that later rolls back, and a rejected audit row
     * cannot drag the business transaction down with it.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, String resourceId) {
        write(CollegeContext.getCollegeId(), UserContext.getUserId(), action, resourceType, resourceId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, UUID resourceId) {
        write(CollegeContext.getCollegeId(), UserContext.getUserId(), action, resourceType,
                resourceId == null ? null : resourceId.toString());
    }

    /**
     * Entry point for event listeners: they run after commit on a pooled thread
     * where the request context is gone, so the tenant and the actor come from
     * the event payload instead.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordForCollege(UUID collegeId,
                                 String userId,
                                 String action,
                                 String resourceType,
                                 String resourceId) {
        write(collegeId, userId, action, resourceType, resourceId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String action,
                                                 String resourceType,
                                                 String userId,
                                                 LocalDateTime from,
                                                 LocalDateTime to,
                                                 Pageable pageable) {

        Page<AuditLog> page = auditLogRepository.search(
                tenantGuard.requireCollegeId(),
                normalize(action),
                normalize(resourceType),
                normalize(userId),
                from,
                to,
                pageable);

        return PageResponse.from(page, auditLogMapper::toResponse);
    }

    private void write(UUID collegeId, String userId, String action, String resourceType, String resourceId) {
        if (collegeId == null) {
            // An entry with no tenant could never be read back through the
            // college-scoped API, so it is dropped rather than orphaned.
            log.debug("Dropped audit entry {} on {}: no college in scope", action, resourceType);
            return;
        }
        if (normalize(action) == null || normalize(resourceType) == null) {
            log.warn("Dropped an audit entry without an action or a resource type");
            return;
        }

        AuditLog entry = new AuditLog();
        entry.setCollegeId(collegeId);
        entry.setUserId(normalize(userId));
        entry.setAction(clip(normalize(action)));
        entry.setResourceType(clip(normalize(resourceType)));
        entry.setResourceId(clip(normalize(resourceId)));

        auditLogRepository.save(entry);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String clip(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_TEXT_LENGTH ? value : value.substring(0, MAX_TEXT_LENGTH);
    }
}
