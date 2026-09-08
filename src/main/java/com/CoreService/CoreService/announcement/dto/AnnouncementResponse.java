package com.CoreService.CoreService.announcement.dto;

import com.CoreService.CoreService.announcement.enums.AnnouncementTarget;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementResponse(UUID id,
                                   String title,
                                   String body,
                                   AnnouncementTarget target,
                                   UUID targetId,
                                   String targetUserId,
                                   String createdBy,
                                   boolean published,
                                   LocalDateTime publishedAt,
                                   LocalDateTime createdAt) {
}
