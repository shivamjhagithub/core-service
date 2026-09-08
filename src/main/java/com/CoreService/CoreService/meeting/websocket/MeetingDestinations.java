package com.CoreService.CoreService.meeting.websocket;

import com.CoreService.CoreService.common.websocket.WebSocketDestinations;

/**
 * The meeting module's STOMP contract in one place, following the same pattern
 * as {@link WebSocketDestinations}: the signaling controller maps
 * {@link #SIGNAL_MAPPING} and the join descriptor advertises
 * {@link #SIGNAL_DESTINATION}, so a client is never told to send to a
 * destination nothing handles.
 */
public final class MeetingDestinations {

    /** {@code @MessageMapping} path, relative to the application prefix. */
    public static final String SIGNAL_MAPPING = "/meeting/signal";

    /** Full destination a client sends signaling frames to. */
    public static final String SIGNAL_DESTINATION =
            WebSocketDestinations.APP_PREFIX + SIGNAL_MAPPING;

    /** Where a client receives signaling addressed to it personally. */
    public static final String SIGNAL_QUEUE =
            WebSocketDestinations.USER_PREFIX + WebSocketDestinations.USER_SIGNAL_QUEUE;

    private MeetingDestinations() {
    }
}
