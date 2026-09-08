package com.CoreService.CoreService.classroom.service;

import com.CoreService.CoreService.classroom.dto.ClassroomDetailResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomMemberResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomPermissionResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomResponse;
import com.CoreService.CoreService.classroom.dto.CreateClassroomRequest;
import com.CoreService.CoreService.classroom.dto.UpdateClassroomRequest;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionMode;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
import com.CoreService.CoreService.common.response.PageResponse;

import java.util.List;
import java.util.UUID;

/**
 * Classroom administration used by the classroom module's own endpoints.
 * Cross-module authorization lives in {@link ClassroomAccessService}.
 */
public interface ClassroomService {

    ClassroomDetailResponse createClassroom(CreateClassroomRequest request);

    ClassroomDetailResponse getClassroom(UUID classroomId);

    ClassroomDetailResponse updateClassroom(UUID classroomId, UpdateClassroomRequest request);

    PageResponse<ClassroomResponse> myClassrooms(int page, int size, boolean archived);

    PageResponse<ClassroomResponse> allClassrooms(int page, int size, boolean archived);

    void archiveClassroom(UUID classroomId);

    void unarchiveClassroom(UUID classroomId);

    void deleteClassroom(UUID classroomId);

    List<ClassroomMemberResponse> addStudents(UUID classroomId, List<String> userIds);

    List<ClassroomMemberResponse> addCoTeachers(UUID classroomId, List<String> userIds);

    void removeMember(UUID classroomId, String userId);

    List<ClassroomMemberResponse> listMembers(UUID classroomId);

    List<ClassroomPermissionResponse> getPermissions(UUID classroomId);

    ClassroomPermissionResponse updatePermission(UUID classroomId,
                                                 ClassroomPermissionType permissionType,
                                                 ClassroomPermissionMode mode);
}
