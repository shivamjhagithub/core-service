package com.CoreService.CoreService.notification.service;

import com.CoreService.CoreService.notification.enums.DevicePlatform;

import java.util.Map;

/**
 * Extension point for a real push transport (FCM, APNs, web push). Exactly one
 * implementation is active at a time, selected by {@code app.push.*}; the rest
 * of the module never learns which one it is.
 */
public interface PushNotificationProvider {

    /** Provider identity, used in delivery logs. */
    String name();

    void send(String deviceToken, DevicePlatform platform, String title, String body, Map<String, String> data);
}
