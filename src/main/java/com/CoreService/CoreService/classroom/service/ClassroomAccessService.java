package com.CoreService.CoreService.classroom.service;

import com.CoreService.CoreService.classroom.dto.ClassroomRef;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;

import java.util.List;
import java.util.UUID;

/**
 * The classroom module's published API. Assignment, material, attendance, chat
 * and meeting modules authorize through this interface instead of reaching into
 * classroom tables themselves.
 */
public interface ClassroomAccessService {

    /**
     * Loads a classroom after checking it belongs to the caller's college.
     *
     * @throws com.CoreService.CoreService.common.exception.ResourceNotFoundException
     *         when the classroom does not exist in the caller's college
     */
    ClassroomRef requireClassroom(UUID classroomId);

    boolean isMember(UUID classroomId, String userId);

    boolean isTeacher(UUID classroomId, String userId);

    /**
     * Context-free membership check for callers that run outside a servlet
     * request, such as WebSocket subscription authorization.
     */
    boolean isMemberOf(UUID collegeId, UUID classroomId, String userId);

    /**
     * @throws com.CoreService.CoreService.common.exception.ForbiddenException
     *         when the current user is not a member of the classroom
     */
    void assertMember(UUID classroomId);

    /**
     * @throws com.CoreService.CoreService.common.exception.ForbiddenException
     *         when the current user is not a teacher of the classroom
     */
    void assertTeacher(UUID classroomId);

    /**
     * Membership plus the classroom-level capability check configured by the
     * classroom's teachers.
     */
    void assertPermission(UUID classroomId, ClassroomPermissionType permission);

    List<String> memberUserIds(UUID classroomId);

    List<String> studentUserIds(UUID classroomId);

    List<String> teacherUserIds(UUID classroomId);

    /** Classrooms the given user belongs to, within the caller's college. */
    List<UUID> classroomIdsOfUser(String userId);
}
