package com.CoreService.CoreService.classroom.dto;

import com.CoreService.CoreService.classroom.enums.ClassroomMemberType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClassroomMemberResponse(UUID memberId,
                                      String userId,
                                      String userName,
                                      ClassroomMemberType memberType,
                                      LocalDateTime joinedAt) {
}
