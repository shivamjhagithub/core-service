package com.CoreService.CoreService.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddClassroomMembersRequest(

        @NotEmpty(message = "At least one user id is required")
        @Size(max = 200, message = "At most 200 users can be added at once")
        List<@NotBlank(message = "User id must not be blank") String> userIds) {
}
