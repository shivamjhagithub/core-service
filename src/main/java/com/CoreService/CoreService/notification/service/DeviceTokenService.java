package com.CoreService.CoreService.notification.service;

import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.notification.dto.DeviceTokenRegistrationRequest;
import com.CoreService.CoreService.notification.dto.DeviceTokenResponse;
import com.CoreService.CoreService.notification.entity.DeviceToken;
import com.CoreService.CoreService.notification.mapper.DeviceTokenMapper;
import com.CoreService.CoreService.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private static final String RESOURCE_TYPE = "Device token";

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenMapper deviceTokenMapper;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;

    /**
     * Upsert on the token itself. A device that is signed into by somebody else
     * is rebound to the new owner instead of producing a second row, so a push
     * never reaches the previous user of that handset.
     */
    @Transactional
    public DeviceTokenResponse register(DeviceTokenRegistrationRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.NOTIFICATION);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();
        String token = request.token().trim();

        Optional<DeviceToken> existing = deviceTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            DeviceToken deviceToken = existing.get();
            if (collegeId.equals(deviceToken.getCollegeId())) {
                deviceToken.setUserId(userId);
                deviceToken.setPlatform(request.platform());
                deviceToken.setActive(true);
                return deviceTokenMapper.toResponse(deviceToken);
            }
            // The tenant of a row is immutable, so a device that moved to another
            // college is re-created rather than updated in place.
            deviceTokenRepository.delete(deviceToken);
            deviceTokenRepository.flush();
        }

        DeviceToken deviceToken = new DeviceToken();
        deviceToken.setCollegeId(collegeId);
        deviceToken.setUserId(userId);
        deviceToken.setToken(token);
        deviceToken.setPlatform(request.platform());
        deviceToken.setActive(true);

        return deviceTokenMapper.toResponse(deviceTokenRepository.save(deviceToken));
    }

    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> myDeviceTokens() {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.NOTIFICATION);
        return deviceTokenRepository
                .findByCollegeIdAndUserId(tenantGuard.requireCollegeId(), tenantGuard.requireUserId())
                .stream()
                .map(deviceTokenMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deactivate(UUID deviceTokenId) {
        requireOwnDeviceToken(deviceTokenId).setActive(false);
    }

    @Transactional
    public void delete(UUID deviceTokenId) {
        deviceTokenRepository.delete(requireOwnDeviceToken(deviceTokenId));
    }

    private DeviceToken requireOwnDeviceToken(UUID deviceTokenId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.NOTIFICATION);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();

        DeviceToken deviceToken = deviceTokenRepository.findByIdAndCollegeId(deviceTokenId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, deviceTokenId));

        // Somebody else's device is reported as missing rather than forbidden:
        // a caller must not be able to probe which handsets are registered.
        if (!userId.equals(deviceToken.getUserId())) {
            throw new ResourceNotFoundException(RESOURCE_TYPE, deviceTokenId);
        }
        return deviceToken;
    }
}
