package com.CoreService.CoreService.notification.repository;

import com.CoreService.CoreService.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    /**
     * The only lookup that is not college-scoped, and deliberately so: the push
     * token is a globally unique device identity, so registration has to see a
     * row that still belongs to whoever used the device before.
     */
    Optional<DeviceToken> findByToken(String token);

    Optional<DeviceToken> findByIdAndCollegeId(UUID id, UUID collegeId);

    List<DeviceToken> findByCollegeIdAndUserId(UUID collegeId, String userId);

    List<DeviceToken> findByCollegeIdAndUserIdInAndActiveTrue(UUID collegeId, Collection<String> userIds);
}
