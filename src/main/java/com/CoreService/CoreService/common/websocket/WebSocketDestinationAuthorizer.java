package com.CoreService.CoreService.common.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Decides whether a session may subscribe to a destination.
 * <p>
 * Deny by default: a destination nobody claims is not subscribable. This is
 * what stops a user from listening to another college's or another user's
 * traffic by guessing a topic name.
 */
@Component
public class WebSocketDestinationAuthorizer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDestinationAuthorizer.class);

    private final List<StompSubscriptionRule> rules;

    public WebSocketDestinationAuthorizer(List<StompSubscriptionRule> rules) {
        this.rules = rules;
    }

    public boolean isAllowed(String destination, StompPrincipal principal) {
        if (destination == null || principal == null) {
            return false;
        }

        // Spring rewrites /user/** subscriptions to the session's own queue, so
        // a client can only ever reach its own user destinations.
        if (destination.startsWith(WebSocketDestinations.USER_PREFIX + WebSocketDestinations.QUEUE_PREFIX)
                || destination.startsWith(WebSocketDestinations.QUEUE_PREFIX)) {
            return true;
        }

        if (destination.startsWith(WebSocketDestinations.COLLEGE_TOPIC_PREFIX)) {
            UUID requested = parseTrailingUuid(destination, WebSocketDestinations.COLLEGE_TOPIC_PREFIX);
            return requested != null && requested.equals(principal.getCollegeId());
        }

        for (StompSubscriptionRule rule : rules) {
            if (rule.supports(destination)) {
                return rule.isAllowed(destination, principal);
            }
        }

        log.debug("Denied subscription to unclaimed destination {}", destination);
        return false;
    }

    public static UUID parseTrailingUuid(String destination, String prefix) {
        String remainder = destination.substring(prefix.length());
        int slash = remainder.indexOf('/');
        if (slash >= 0) {
            remainder = remainder.substring(0, slash);
        }
        try {
            return UUID.fromString(remainder);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
