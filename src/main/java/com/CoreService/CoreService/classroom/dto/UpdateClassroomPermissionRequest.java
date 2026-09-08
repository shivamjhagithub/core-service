package com.CoreService.CoreService.classroom.dto;

import com.CoreService.CoreService.classroom.enums.ClassroomPermissionMode;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
import jakarta.validation.constraints.NotNull;

public record UpdateClassroomPermissionRequest(

        @NotNull(message = "Permission type is required")
        ClassroomPermissionType permissionType,

        @NotNull(message = "Permission mode is required")
        ClassroomPermissionMode mode) {
}
