package com.CoreService.CoreService.announcement.dto;

import com.CoreService.CoreService.announcement.enums.AnnouncementTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @param targetId     department id for a DEPARTMENT announcement, classroom id
 *                     for a CLASSROOM announcement, null otherwise
 * @param targetUserId recipient of a USER announcement, null otherwise
 * @param publishNow   publish in the same request instead of leaving a draft
 */
public record AnnouncementRequest(@NotBlank @Size(max = 200) String title,
                                  @NotBlank @Size(max = 4000) String body,
                                  @NotNull AnnouncementTarget target,
                                  UUID targetId,
                                  @Size(max = 64) String targetUserId,
                                  boolean publishNow) {
}
