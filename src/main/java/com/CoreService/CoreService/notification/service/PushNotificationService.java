package com.CoreService.CoreService.notification.service;

import com.CoreService.CoreService.notification.entity.DeviceToken;
import com.CoreService.CoreService.notification.enums.NotificationType;
import com.CoreService.CoreService.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Push leg of the fan-out. It resolves the recipients' active device tokens and
 * hands them to whichever provider is configured; with no provider on the
 * context it does nothing at all, and it never throws, because a push failure
 * must not affect the business operation that produced the notification.
 */
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final ObjectProvider<PushNotificationProvider> providers;

    @Transactional(readOnly = true)
    public void push(UUID collegeId,
                     Collection<String> recipientUserIds,
                     NotificationType type,
                     String title,
                     String body,
                     UUID referenceId) {

        if (collegeId == null || recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }

        PushNotificationProvider provider = providers.getIfUnique();
        if (provider == null) {
            return;
        }

        try {
            List<DeviceToken> tokens =
                    deviceTokenRepository.findByCollegeIdAndUserIdInAndActiveTrue(collegeId, recipientUserIds);
            if (tokens.isEmpty()) {
                return;
            }

            Map<String, String> data = payloadData(type, referenceId);
            for (DeviceToken deviceToken : tokens) {
                try {
                    provider.send(deviceToken.getToken(), deviceToken.getPlatform(), title, body, data);
                } catch (RuntimeException ex) {
                    log.warn("Provider {} could not deliver push to device token {}: {}",
                            provider.name(), deviceToken.getId(), ex.toString());
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Push fan-out failed for college {}: {}", collegeId, ex.toString());
        }
    }

    private Map<String, String> payloadData(NotificationType type, UUID referenceId) {
        Map<String, String> data = new HashMap<>(2);
        if (type != null) {
            data.put("type", type.name());
        }
        if (referenceId != null) {
            data.put("referenceId", referenceId.toString());
        }
        return data;
    }
}
