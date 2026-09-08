package com.CoreService.CoreService.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker for in-process events published between modules.
 * <p>
 * Every event carries its tenant so listeners (and Kafka consumers, which run
 * outside the request thread and therefore outside {@code CollegeContext}) can
 * stay scoped to the right college.
 */
public interface DomainEvent {

    UUID collegeId();

    Instant occurredAt();

    /** Stable key used to make asynchronous processing idempotent. */
    String eventKey();
}
