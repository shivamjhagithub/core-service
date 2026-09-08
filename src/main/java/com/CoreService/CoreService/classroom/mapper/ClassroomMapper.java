package com.CoreService.CoreService.classroom.mapper;

import com.CoreService.CoreService.classroom.dto.ClassroomDetailResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomMemberResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomPermissionResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomRef;
import com.CoreService.CoreService.classroom.dto.ClassroomResponse;
import com.CoreService.CoreService.classroom.entity.Classroom;
import com.CoreService.CoreService.classroom.entity.ClassroomMember;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionMode;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ClassroomMapper {

    public ClassroomRef toRef(Classroom classroom) {
        return new ClassroomRef(classroom.getId(),
                classroom.getCollegeId(),
                classroom.getName(),
                classroom.isActive());
    }

    public ClassroomResponse toResponse(Classroom classroom, long memberCount) {
        return new ClassroomResponse(classroom.getId(),
                classroom.getName(),
                classroom.getDescription(),
                classroom.getCreatedBy(),
                classroom.isActive(),
                classroom.isArchived(),
                memberCount,
                classroom.getCreatedAt(),
                classroom.getUpdatedAt());
    }

    public ClassroomDetailResponse toDetailResponse(Classroom classroom,
                                                    List<ClassroomMemberResponse> members,
                                                    List<ClassroomPermissionResponse> permissions) {
        return new ClassroomDetailResponse(classroom.getId(),
                classroom.getName(),
                classroom.getDescription(),
                classroom.getCreatedBy(),
                classroom.isActive(),
                classroom.isArchived(),
                classroom.getCreatedAt(),
                classroom.getUpdatedAt(),
                members,
                permissions);
    }

    public ClassroomMemberResponse toMemberResponse(ClassroomMember member, String userName) {
        return new ClassroomMemberResponse(member.getId(),
                member.getUserId(),
                userName,
                member.getMemberType(),
                member.getJoinedAt());
    }

    public List<ClassroomMemberResponse> toMemberResponses(List<ClassroomMember> members,
                                                           Map<String, String> userNames) {
        return members.stream()
                .map(member -> toMemberResponse(member, userNames.get(member.getUserId())))
                .toList();
    }

    public ClassroomPermissionResponse toPermissionResponse(ClassroomPermissionType permissionType,
                                                            ClassroomPermissionMode mode) {
        return new ClassroomPermissionResponse(permissionType, mode);
    }
}
