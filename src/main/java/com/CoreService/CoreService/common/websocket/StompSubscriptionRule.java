package com.CoreService.CoreService.common.websocket;

/**
 * Authorization rule for a family of STOMP destinations.
 * <p>
 * Each module contributes a bean for the destinations it owns, which keeps
 * subscription authorization next to the data it protects. Destinations that
 * match no rule are denied.
 */
public interface StompSubscriptionRule {

    boolean supports(String destination);

    boolean isAllowed(String destination, StompPrincipal principal);
}
