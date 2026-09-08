package com.CoreService.CoreService.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base type for tenant-scoped data. {@code collegeId} is the logical isolation
 * key: it is always populated from the authenticated request context, never
 * from client input, and every query touching such a table must filter on it.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "college_id", nullable = false, updatable = false)
    private UUID collegeId;
}
