package com.CoreService.CoreService.classroom.websocket;

import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.websocket.StompPrincipal;
import com.CoreService.CoreService.common.websocket.StompSubscriptionRule;
import com.CoreService.CoreService.common.websocket.WebSocketDestinationAuthorizer;
import com.CoreService.CoreService.common.websocket.WebSocketDestinations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Only members of a classroom may listen to its topic. The college comes from
 * the authenticated STOMP session, so a guessed classroom id from another
 * tenant can never match.
 */
@Component
@RequiredArgsConstructor
public class ClassroomSubscriptionRule implements StompSubscriptionRule {

    private final ClassroomAccessService classroomAccessService;

    @Override
    public boolean supports(String destination) {
        return destination != null && destination.startsWith(WebSocketDestinations.CLASSROOM_TOPIC_PREFIX);
    }

    @Override
    public boolean isAllowed(String destination, StompPrincipal principal) {
        UUID classroomId = WebSocketDestinationAuthorizer.parseTrailingUuid(
                destination, WebSocketDestinations.CLASSROOM_TOPIC_PREFIX);

        if (classroomId == null) {
            return false;
        }
        return classroomAccessService.isMemberOf(principal.getCollegeId(), classroomId, principal.getUserId());
    }
}
