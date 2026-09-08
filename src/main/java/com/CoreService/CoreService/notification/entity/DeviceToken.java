package com.CoreService.CoreService.notification.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import com.CoreService.CoreService.notification.enums.DevicePlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Push handle of one physical device. The token is unique platform-wide, so a
 * device that is handed to another user is rebound rather than duplicated.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_tokens_token", columnNames = "token"),
        indexes = {
                @Index(name = "idx_device_tokens_college_id", columnList = "college_id"),
                @Index(name = "idx_device_tokens_user_id", columnList = "user_id")
        })
public class DeviceToken extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "platform", nullable = false, length = 16)
    private DevicePlatform platform;

    @Column(name = "active", nullable = false)
    private boolean active;
}
