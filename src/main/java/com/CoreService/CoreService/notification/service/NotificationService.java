package com.CoreService.CoreService.notification.service;

import com.CoreService.CoreService.common.exception.ForbiddenException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.common.websocket.WebSocketDestinations;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.notification.dto.NotificationPayload;
import com.CoreService.CoreService.notification.dto.NotificationRequest;
import com.CoreService.CoreService.notification.dto.NotificationResponse;
import com.CoreService.CoreService.notification.entity.Notification;
import com.CoreService.CoreService.notification.enums.NotificationType;
import com.CoreService.CoreService.notification.mapper.NotificationMapper;
import com.CoreService.CoreService.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final String RESOURCE_TYPE = "Notification";
    private static final int MAX_BODY_LENGTH = 1000;

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final PushNotificationService pushNotificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;

    /**
     * Storing every recipient's row in one transaction keeps the inbox
     * consistent; delivery afterwards is best effort, because a broken socket
     * or an unreachable push provider must not undo the persisted inbox nor
     * propagate back into the business operation that triggered the fan-out.
     */
    @Override
    @Transactional
    public void dispatch(NotificationRequest request) {
        if (request == null) {
            return;
        }

        UUID collegeId = request.collegeId();
        if (collegeId == null || request.type() == null || isBlank(request.title())) {
            log.warn("Discarded an incomplete notification request of type {}", request.type());
            return;
        }

        List<String> recipients = distinct(request.recipientUserIds());
        if (recipients.isEmpty()) {
            return;
        }

        List<Notification> rows = new ArrayList<>(recipients.size());
        for (String recipientUserId : recipients) {
            Notification notification = new Notification();
            notification.setCollegeId(collegeId);
            notification.setRecipientUserId(recipientUserId);
            notification.setType(request.type());
            notification.setTitle(request.title());
            notification.setBody(truncate(request.body()));
            notification.setReferenceId(request.referenceId());
            notification.setRead(false);
            rows.add(notification);
        }

        for (Notification saved : notificationRepository.saveAll(rows)) {
            deliverOverWebSocket(saved);
        }

        pushNotificationService.push(collegeId, recipients, request.type(), request.title(),
                truncate(request.body()), request.referenceId());
    }

    /**
     * Live-only signal for events that identify no audience. Nothing is stored,
     * because there is no recipient to store it against; the college topic is
     * already restricted to members of that college.
     */
    public void broadcastToCollege(UUID collegeId,
                                   NotificationType type,
                                   String title,
                                   String body,
                                   UUID referenceId) {

        if (collegeId == null || type == null || isBlank(title)) {
            return;
        }

        NotificationPayload payload = new NotificationPayload(null, collegeId, type, title,
                truncate(body), referenceId, false, LocalDateTime.now());
        try {
            messagingTemplate.convertAndSend(WebSocketDestinations.collegeTopic(collegeId), payload);
        } catch (RuntimeException ex) {
            log.warn("WebSocket broadcast to college {} failed: {}", collegeId, ex.toString());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> myNotifications(boolean unreadOnly, Pageable pageable) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.NOTIFICATION);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();

        Page<Notification> page = unreadOnly
                ? notificationRepository.findByCollegeIdAndRecipientUserIdAndReadFalse(collegeId, userId, pageable)
                : notificationRepository.findByCollegeIdAndRecipientUserId(collegeId, userId, pageable);

        return PageResponse.from(page, notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.NOTIFICATION);
        return notificationRepository.countByCollegeIdAndRecipientUserIdAndReadFalse(
                tenantGuard.requireCollegeId(), tenantGuard.requireUserId());
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.NOTIFICATION);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();

        Notification notification = notificationRepository.findByIdAndCollegeId(notificationId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, notificationId));

        if (!userId.equals(notification.getRecipientUserId())) {
            throw new ForbiddenException("A notification can only be read by the user it was addressed to");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }

        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public int markAllRead() {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.NOTIFICATION);
        return notificationRepository.markAllRead(tenantGuard.requireCollegeId(),
                tenantGuard.requireUserId(), LocalDateTime.now());
    }

    private void deliverOverWebSocket(Notification notification) {
        try {
            messagingTemplate.convertAndSendToUser(notification.getRecipientUserId(),
                    WebSocketDestinations.USER_NOTIFICATION_QUEUE, notificationMapper.toPayload(notification));
        } catch (RuntimeException ex) {
            log.warn("WebSocket delivery of notification {} to user {} failed: {}",
                    notification.getId(), notification.getRecipientUserId(), ex.toString());
        }
    }

    private List<String> distinct(Collection<String> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String recipientUserId : recipientUserIds) {
            if (!isBlank(recipientUserId)) {
                unique.add(recipientUserId);
            }
        }
        return List.copyOf(unique);
    }

    private String truncate(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= MAX_BODY_LENGTH ? body : body.substring(0, MAX_BODY_LENGTH);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
