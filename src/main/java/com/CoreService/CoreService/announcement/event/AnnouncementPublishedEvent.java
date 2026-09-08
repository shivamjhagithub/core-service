package com.CoreService.CoreService.announcement.event;

import com.CoreService.CoreService.common.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnnouncementPublishedEvent(UUID collegeId,
                                         UUID announcementId,
                                         String title,
                                         List<String> recipientUserIds,
                                         Instant occurredAt) implements DomainEvent {

    @Override
    public String eventKey() {
        return "announcement-published:" + announcementId;
    }
}
