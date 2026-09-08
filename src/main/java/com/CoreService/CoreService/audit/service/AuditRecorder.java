package com.CoreService.CoreService.audit.service;

import java.util.UUID;

/**
 * Audit module's published API. Implementations must never persist passwords,
 * tokens or secrets.
 */
public interface AuditRecorder {

    void record(String action, String resourceType, String resourceId);

    void record(String action, String resourceType, UUID resourceId);
}
