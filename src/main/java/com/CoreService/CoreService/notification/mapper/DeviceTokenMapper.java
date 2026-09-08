package com.CoreService.CoreService.notification.mapper;

import com.CoreService.CoreService.notification.dto.DeviceTokenResponse;
import com.CoreService.CoreService.notification.entity.DeviceToken;
import org.springframework.stereotype.Component;

@Component
public class DeviceTokenMapper {

    public DeviceTokenResponse toResponse(DeviceToken deviceToken) {
        return new DeviceTokenResponse(
                deviceToken.getId(),
                deviceToken.getToken(),
                deviceToken.getPlatform(),
                deviceToken.isActive(),
                deviceToken.getCreatedAt());
    }
}
