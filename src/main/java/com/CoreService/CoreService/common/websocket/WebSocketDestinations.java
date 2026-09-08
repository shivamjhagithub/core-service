package com.CoreService.CoreService.common.websocket;

import java.util.UUID;

/**
 * Single source of truth for STOMP destinations, so producers and the
 * subscription authorizer cannot drift apart.
 */
public final class WebSocketDestinations {

    public static final String APP_PREFIX = "/app";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String USER_PREFIX = "/user";

    public static final String CLASSROOM_TOPIC_PREFIX = "/topic/classroom/";
    public static final String CHAT_ROOM_TOPIC_PREFIX = "/topic/chat/";
    public static final String MEETING_TOPIC_PREFIX = "/topic/meeting/";
    public static final String COLLEGE_TOPIC_PREFIX = "/topic/college/";

    public static final String USER_NOTIFICATION_QUEUE = "/queue/notifications";
    public static final String USER_CHAT_QUEUE = "/queue/chat";
    public static final String USER_SIGNAL_QUEUE = "/queue/signal";

    public static String classroomTopic(UUID classroomId) {
        return CLASSROOM_TOPIC_PREFIX + classroomId;
    }

    public static String chatRoomTopic(UUID chatRoomId) {
        return CHAT_ROOM_TOPIC_PREFIX + chatRoomId;
    }

    public static String meetingTopic(UUID meetingId) {
        return MEETING_TOPIC_PREFIX + meetingId;
    }

    public static String collegeTopic(UUID collegeId) {
        return COLLEGE_TOPIC_PREFIX + collegeId;
    }

    private WebSocketDestinations() {
    }
}
