package com.CoreService.CoreService.notification.service;

import com.CoreService.CoreService.notification.enums.DevicePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default provider so the notification pipeline is complete out of the box:
 * notifications are still stored and delivered over WebSocket, only the push
 * leg is a no-op until a real provider is configured.
 */
@Component
@ConditionalOnProperty(name = "app.push.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPushNotificationProvider implements PushNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(NoopPushNotificationProvider.class);

    @Override
    public String name() {
        return "noop";
    }

    @Override
    public void send(String deviceToken, DevicePlatform platform, String title, String body, Map<String, String> data) {
        log.debug("Push disabled, dropping '{}' for a {} device", title, platform);
    }
}
