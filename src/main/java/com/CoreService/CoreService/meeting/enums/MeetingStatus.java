package com.CoreService.CoreService.meeting.enums;

/**
 * Lifecycle of a meeting. SCHEDULED and LIVE are the only joinable states, and
 * ENDED and CANCELLED are terminal.
 */
public enum MeetingStatus {
    SCHEDULED,
    LIVE,
    ENDED,
    CANCELLED
}
