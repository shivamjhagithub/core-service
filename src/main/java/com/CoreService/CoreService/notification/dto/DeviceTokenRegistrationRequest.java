package com.CoreService.CoreService.notification.dto;

import com.CoreService.CoreService.notification.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceTokenRegistrationRequest(@NotBlank @Size(max = 512) String token,
                                             @NotNull DevicePlatform platform) {
}
