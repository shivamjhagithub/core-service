package com.CoreService.CoreService.notification.dto;

import com.CoreService.CoreService.notification.enums.DevicePlatform;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceTokenResponse(UUID id,
                                  String token,
                                  DevicePlatform platform,
                                  boolean active,
                                  LocalDateTime createdAt) {
}
