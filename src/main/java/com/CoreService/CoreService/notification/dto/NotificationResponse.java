package com.CoreService.CoreService.notification.dto;

import com.CoreService.CoreService.notification.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(UUID id,
                                   NotificationType type,
                                   String title,
                                   String body,
                                   UUID referenceId,
                                   boolean read,
                                   LocalDateTime readAt,
                                   LocalDateTime createdAt) {
}
