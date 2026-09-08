package com.CoreService.CoreService.common.websocket;

import lombok.Getter;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Identity attached to a STOMP session once its CONNECT frame has been
 * authenticated. Everything the server needs about the caller comes from here,
 * never from frame payloads.
 */
@Getter
public class StompPrincipal implements Principal {

    private final String userId;
    private final UUID collegeId;
    private final List<String> roles;
    private final List<String> permissions;

    public StompPrincipal(String userId, UUID collegeId, List<String> roles, List<String> permissions) {
        this.userId = userId;
        this.collegeId = collegeId;
        this.roles = List.copyOf(roles);
        this.permissions = List.copyOf(permissions);
    }

    /** Spring routes {@code /user/**} destinations by this value. */
    @Override
    public String getName() {
        return userId;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
