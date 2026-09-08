package com.CoreService.CoreService.chat.dto;

import com.CoreService.CoreService.chat.enums.ChatPermissionMode;
import jakarta.validation.constraints.NotNull;

public record UpdatePermissionModeRequest(@NotNull ChatPermissionMode permissionMode) {
}
