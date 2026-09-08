package com.CoreService.CoreService.announcement.mapper;

import com.CoreService.CoreService.announcement.dto.AnnouncementResponse;
import com.CoreService.CoreService.announcement.entity.Announcement;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementMapper {

    public AnnouncementResponse toResponse(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getTarget(),
                announcement.getTargetId(),
                announcement.getTargetUserId(),
                announcement.getCreatedBy(),
                announcement.isPublished(),
                announcement.getPublishedAt(),
                announcement.getCreatedAt());
    }
}
