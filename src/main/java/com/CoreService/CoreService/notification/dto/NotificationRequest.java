package com.CoreService.CoreService.notification.dto;

import com.CoreService.CoreService.notification.enums.NotificationType;
import lombok.Builder;

import java.util.Collection;
import java.util.UUID;

/**
 * Instruction to fan a notification out to a set of users of one college.
 */
@Builder
public record NotificationRequest(UUID collegeId,
                                  Collection<String> recipientUserIds,
                                  NotificationType type,
                                  String title,
                                  String body,
                                  /* Domain object the notification points at, e.g. an assignment id. */
                                  UUID referenceId) {
}
