package com.CoreService.CoreService.classroom.dto;

import com.CoreService.CoreService.classroom.enums.ClassroomPermissionMode;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;

public record ClassroomPermissionResponse(ClassroomPermissionType permissionType,
                                          ClassroomPermissionMode mode) {
}
