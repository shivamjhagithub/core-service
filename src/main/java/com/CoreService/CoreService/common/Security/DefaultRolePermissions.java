package com.CoreService.CoreService.common.security;

import java.util.List;
import java.util.Map;

/**
 * Permission sets granted to the roles created with a new college. A college
 * admin can edit these roles afterwards; nothing in the code depends on the
 * sets staying as seeded.
 */
public final class DefaultRolePermissions {

    public static final List<String> COLLEGE_ADMIN = List.of(
            "CREATE_USER", "UPDATE_USER", "DELETE_USER", "VIEW_USER", "VIEW_ALL_COLLEGE_USER",
            "CREATE_ROLE", "UPDATE_ROLE", "DELETE_ROLE", "VIEW_ROLE", "ASSIGN_ROLE", "REMOVE_ROLE",
            "GET_PERMISSION", "ASSIGN_PERMISSION", "REMOVE_PERMISSION",
            "VIEW_SETTINGS", "UPDATE_SETTINGS", "VIEW_REPORT", "EXPORT_REPORT",
            Permissions.ACADEMIC_MANAGE, Permissions.ACADEMIC_VIEW,
            Permissions.STUDENT_CREATE, Permissions.STUDENT_UPDATE, Permissions.STUDENT_VIEW,
            Permissions.TEACHER_CREATE, Permissions.TEACHER_UPDATE, Permissions.TEACHER_VIEW,
            Permissions.CLASSROOM_CREATE, Permissions.CLASSROOM_UPDATE, Permissions.CLASSROOM_DELETE,
            Permissions.CLASSROOM_VIEW, Permissions.CLASSROOM_MEMBER_MANAGE,
            Permissions.CLASSROOM_PERMISSION_MANAGE,
            Permissions.ATTENDANCE_VIEW, Permissions.SYLLABUS_VIEW,
            Permissions.ASSIGNMENT_VIEW, Permissions.MATERIAL_VIEW,
            Permissions.ANNOUNCEMENT_CREATE, Permissions.ANNOUNCEMENT_DELETE, Permissions.ANNOUNCEMENT_VIEW,
            Permissions.CHAT_VIEW, Permissions.MEETING_VIEW,
            Permissions.NOTIFICATION_VIEW, Permissions.DASHBOARD_VIEW, Permissions.AUDIT_VIEW);

    public static final List<String> TEACHER = List.of(
            "VIEW_USER", "VIEW_PROFILE", "UPDATE_PROFILE",
            Permissions.ACADEMIC_VIEW,
            Permissions.STUDENT_VIEW, Permissions.TEACHER_VIEW,
            Permissions.CLASSROOM_CREATE, Permissions.CLASSROOM_UPDATE, Permissions.CLASSROOM_VIEW,
            Permissions.CLASSROOM_MEMBER_MANAGE, Permissions.CLASSROOM_PERMISSION_MANAGE,
            Permissions.ATTENDANCE_CREATE, Permissions.ATTENDANCE_UPDATE, Permissions.ATTENDANCE_VIEW,
            Permissions.SYLLABUS_MANAGE, Permissions.SYLLABUS_VIEW,
            Permissions.ASSIGNMENT_CREATE, Permissions.ASSIGNMENT_UPDATE, Permissions.ASSIGNMENT_DELETE,
            Permissions.ASSIGNMENT_VIEW, Permissions.ASSIGNMENT_GRADE,
            Permissions.MATERIAL_UPLOAD, Permissions.MATERIAL_DELETE, Permissions.MATERIAL_VIEW,
            Permissions.ANNOUNCEMENT_CREATE, Permissions.ANNOUNCEMENT_DELETE, Permissions.ANNOUNCEMENT_VIEW,
            Permissions.CHAT_SEND, Permissions.CHAT_VIEW,
            Permissions.MEETING_CREATE, Permissions.MEETING_UPDATE, Permissions.MEETING_JOIN,
            Permissions.MEETING_VIEW,
            Permissions.NOTIFICATION_VIEW, Permissions.DASHBOARD_VIEW);

    public static final List<String> STUDENT = List.of(
            "VIEW_PROFILE", "UPDATE_PROFILE",
            Permissions.ACADEMIC_VIEW,
            Permissions.CLASSROOM_VIEW,
            Permissions.ATTENDANCE_VIEW,
            Permissions.SYLLABUS_VIEW,
            Permissions.ASSIGNMENT_VIEW, Permissions.ASSIGNMENT_SUBMIT,
            Permissions.MATERIAL_VIEW,
            Permissions.ANNOUNCEMENT_VIEW,
            Permissions.CHAT_SEND, Permissions.CHAT_VIEW,
            Permissions.MEETING_JOIN, Permissions.MEETING_VIEW,
            Permissions.NOTIFICATION_VIEW, Permissions.DASHBOARD_VIEW);

    public static final Map<String, List<String>> BY_ROLE = Map.of(
            RoleNames.COLLEGE_ADMIN, COLLEGE_ADMIN,
            RoleNames.TEACHER, TEACHER,
            RoleNames.STUDENT, STUDENT);

    /** Modules a new college starts with. */
    public static final List<String> DEFAULT_COLLEGE_MODULES = List.of(
            ModuleCodes.USER_MANAGEMENT,
            ModuleCodes.ACADEMIC,
            ModuleCodes.CLASSROOM,
            ModuleCodes.ATTENDANCE,
            ModuleCodes.SYLLABUS,
            ModuleCodes.ASSIGNMENT,
            ModuleCodes.STUDY_MATERIAL,
            ModuleCodes.ANNOUNCEMENT,
            ModuleCodes.CHAT,
            ModuleCodes.VIDEO_CONFERENCE,
            ModuleCodes.NOTIFICATION);

    private DefaultRolePermissions() {
    }
}
