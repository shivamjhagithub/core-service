package com.CoreService.CoreService.assignment.dto;

import java.util.UUID;

/**
 * Flat projection of an attachment together with the id of its owner, so a
 * page of assignments or submissions resolves its attachments in one query
 * without initialising a lazy reference per row.
 */
public record AttachmentRow(UUID ownerId, UUID fileId) {
}
