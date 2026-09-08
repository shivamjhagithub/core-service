package com.CoreService.CoreService.notification.dto;

import com.CoreService.CoreService.notification.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What a subscribed client receives over STOMP. Kept separate from
 * {@link NotificationResponse} because live frames also carry college-wide
 * signals that were never stored against a single recipient, and therefore
 * have no id.
 */
public record NotificationPayload(UUID id,
                                  UUID collegeId,
                                  NotificationType type,
                                  String title,
                                  String body,
                                  UUID referenceId,
                                  boolean read,
                                  LocalDateTime createdAt) {
}
