package com.CoreService.CoreService.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record DashboardAnnouncementItem(UUID announcementId,
                                        String title,
                                        String target,
                                        Instant publishedAt) {
}
