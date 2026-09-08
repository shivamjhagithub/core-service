package com.CoreService.CoreService.notification.controller;

import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.notification.dto.DeviceTokenRegistrationRequest;
import com.CoreService.CoreService.notification.dto.DeviceTokenResponse;
import com.CoreService.CoreService.notification.dto.NotificationResponse;
import com.CoreService.CoreService.notification.dto.UnreadCountResponse;
import com.CoreService.CoreService.notification.service.DeviceTokenService;
import com.CoreService.CoreService.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<PageResponse<NotificationResponse>> myNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ApiResponse.success(notificationService.myNotifications(unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.success(new UnreadCountResponse(notificationService.unreadCount()));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID notificationId) {
        return ApiResponse.success("Notification marked as read", notificationService.markRead(notificationId));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<Integer> markAllRead() {
        return ApiResponse.success("Notifications marked as read", notificationService.markAllRead());
    }

    @PostMapping("/device-tokens")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<DeviceTokenResponse> registerDeviceToken(
            @Valid @RequestBody DeviceTokenRegistrationRequest request) {

        return ApiResponse.success("Device registered", deviceTokenService.register(request));
    }

    @GetMapping("/device-tokens")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<List<DeviceTokenResponse>> myDeviceTokens() {
        return ApiResponse.success(deviceTokenService.myDeviceTokens());
    }

    @PatchMapping("/device-tokens/{deviceTokenId}/deactivate")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<Void> deactivateDeviceToken(@PathVariable UUID deviceTokenId) {
        deviceTokenService.deactivate(deviceTokenId);
        return ApiResponse.message("Device deactivated");
    }

    @DeleteMapping("/device-tokens/{deviceTokenId}")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<Void> deleteDeviceToken(@PathVariable UUID deviceTokenId) {
        deviceTokenService.delete(deviceTokenId);
        return ApiResponse.message("Device removed");
    }
}
