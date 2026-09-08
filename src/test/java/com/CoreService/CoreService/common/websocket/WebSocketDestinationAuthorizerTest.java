package com.CoreService.CoreService.common.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketDestinationAuthorizerTest {

    private static final UUID COLLEGE_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COLLEGE_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final WebSocketDestinationAuthorizer authorizer =
            new WebSocketDestinationAuthorizer(List.of());

    @Test
    @DisplayName("a college A user cannot subscribe to college B's topic")
    void deniesAnotherCollegesTopic() {
        assertThat(authorizer.isAllowed(WebSocketDestinations.collegeTopic(COLLEGE_B), principal(COLLEGE_A)))
                .isFalse();
    }

    @Test
    void allowsOwnCollegeTopic() {
        assertThat(authorizer.isAllowed(WebSocketDestinations.collegeTopic(COLLEGE_A), principal(COLLEGE_A)))
                .isTrue();
    }

    @Test
    @DisplayName("destinations no module claims are denied")
    void deniesUnclaimedDestinations() {
        assertThat(authorizer.isAllowed("/topic/classroom/" + UUID.randomUUID(), principal(COLLEGE_A)))
                .isFalse();
        assertThat(authorizer.isAllowed("/topic/anything", principal(COLLEGE_A))).isFalse();
    }

    @Test
    void allowsUserScopedQueues() {
        assertThat(authorizer.isAllowed(
                WebSocketDestinations.USER_PREFIX + WebSocketDestinations.USER_NOTIFICATION_QUEUE,
                principal(COLLEGE_A))).isTrue();
    }

    @Test
    void deniesUnauthenticatedSessions() {
        assertThat(authorizer.isAllowed(WebSocketDestinations.collegeTopic(COLLEGE_A), null)).isFalse();
    }

    @Test
    void moduleRuleDecidesForItsOwnDestinations() {
        UUID classroomId = UUID.randomUUID();

        WebSocketDestinationAuthorizer withRule = new WebSocketDestinationAuthorizer(List.of(
                new StompSubscriptionRule() {
                    @Override
                    public boolean supports(String destination) {
                        return destination.startsWith(WebSocketDestinations.CLASSROOM_TOPIC_PREFIX);
                    }

                    @Override
                    public boolean isAllowed(String destination, StompPrincipal principal) {
                        return classroomId.equals(WebSocketDestinationAuthorizer
                                .parseTrailingUuid(destination, WebSocketDestinations.CLASSROOM_TOPIC_PREFIX));
                    }
                }));

        assertThat(withRule.isAllowed(WebSocketDestinations.classroomTopic(classroomId), principal(COLLEGE_A)))
                .isTrue();
        assertThat(withRule.isAllowed(WebSocketDestinations.classroomTopic(UUID.randomUUID()), principal(COLLEGE_A)))
                .isFalse();
    }

    @Test
    void parsesTrailingUuidAndRejectsGarbage() {
        UUID id = UUID.randomUUID();

        assertThat(WebSocketDestinationAuthorizer.parseTrailingUuid(
                WebSocketDestinations.classroomTopic(id), WebSocketDestinations.CLASSROOM_TOPIC_PREFIX))
                .isEqualTo(id);

        assertThat(WebSocketDestinationAuthorizer.parseTrailingUuid(
                WebSocketDestinations.CLASSROOM_TOPIC_PREFIX + "not-a-uuid",
                WebSocketDestinations.CLASSROOM_TOPIC_PREFIX))
                .isNull();
    }

    private StompPrincipal principal(UUID collegeId) {
        return new StompPrincipal("user-1", collegeId, List.of("STUDENT"), List.of("CHAT_VIEW"));
    }
}
