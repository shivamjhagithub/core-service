package com.CoreService.CoreService.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Only the peer is accepted; the caller's own identity comes from the
 * authenticated context.
 */
public record OpenDirectRoomRequest(@NotBlank @Size(max = 255) String otherUserId) {
}
