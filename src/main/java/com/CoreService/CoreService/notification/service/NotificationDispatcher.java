package com.CoreService.CoreService.notification.service;

import com.CoreService.CoreService.notification.dto.NotificationRequest;

/**
 * Notification module's published API: persist, then deliver over WebSocket and
 * push. Callers are normally domain-event listeners rather than services, so a
 * failing notification never rolls back business data.
 */
public interface NotificationDispatcher {

    void dispatch(NotificationRequest request);
}
