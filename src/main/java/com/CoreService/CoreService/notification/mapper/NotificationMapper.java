package com.CoreService.CoreService.notification.mapper;

import com.CoreService.CoreService.notification.dto.NotificationPayload;
import com.CoreService.CoreService.notification.dto.NotificationResponse;
import com.CoreService.CoreService.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }

    public NotificationPayload toPayload(Notification notification) {
        return new NotificationPayload(
                notification.getId(),
                notification.getCollegeId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
