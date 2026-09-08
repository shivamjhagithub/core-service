package com.CoreService.CoreService.common.security;

/**
 * Permission codes granted to roles and asserted by {@code @PreAuthorize}.
 * Keep in sync with the permission seed migration.
 */
public final class Permissions {

    // Classroom
    public static final String CLASSROOM_CREATE = "CLASSROOM_CREATE";
    public static final String CLASSROOM_UPDATE = "CLASSROOM_UPDATE";
    public static final String CLASSROOM_DELETE = "CLASSROOM_DELETE";
    public static final String CLASSROOM_VIEW = "CLASSROOM_VIEW";
    public static final String CLASSROOM_MEMBER_MANAGE = "CLASSROOM_MEMBER_MANAGE";
    public static final String CLASSROOM_PERMISSION_MANAGE = "CLASSROOM_PERMISSION_MANAGE";

    // Academic structure
    public static final String ACADEMIC_MANAGE = "ACADEMIC_MANAGE";
    public static final String ACADEMIC_VIEW = "ACADEMIC_VIEW";

    // Student / teacher records
    public static final String STUDENT_CREATE = "STUDENT_CREATE";
    public static final String STUDENT_UPDATE = "STUDENT_UPDATE";
    public static final String STUDENT_VIEW = "STUDENT_VIEW";
    public static final String TEACHER_CREATE = "TEACHER_CREATE";
    public static final String TEACHER_UPDATE = "TEACHER_UPDATE";
    public static final String TEACHER_VIEW = "TEACHER_VIEW";

    // Attendance
    public static final String ATTENDANCE_CREATE = "ATTENDANCE_CREATE";
    public static final String ATTENDANCE_UPDATE = "ATTENDANCE_UPDATE";
    public static final String ATTENDANCE_VIEW = "ATTENDANCE_VIEW";

    // Syllabus
    public static final String SYLLABUS_MANAGE = "SYLLABUS_MANAGE";
    public static final String SYLLABUS_VIEW = "SYLLABUS_VIEW";

    // Assignment
    public static final String ASSIGNMENT_CREATE = "ASSIGNMENT_CREATE";
    public static final String ASSIGNMENT_UPDATE = "ASSIGNMENT_UPDATE";
    public static final String ASSIGNMENT_DELETE = "ASSIGNMENT_DELETE";
    public static final String ASSIGNMENT_VIEW = "ASSIGNMENT_VIEW";
    public static final String ASSIGNMENT_SUBMIT = "ASSIGNMENT_SUBMIT";
    public static final String ASSIGNMENT_GRADE = "ASSIGNMENT_GRADE";

    // Study material
    public static final String MATERIAL_UPLOAD = "MATERIAL_UPLOAD";
    public static final String MATERIAL_DELETE = "MATERIAL_DELETE";
    public static final String MATERIAL_VIEW = "MATERIAL_VIEW";

    // Announcement
    public static final String ANNOUNCEMENT_CREATE = "ANNOUNCEMENT_CREATE";
    public static final String ANNOUNCEMENT_DELETE = "ANNOUNCEMENT_DELETE";
    public static final String ANNOUNCEMENT_VIEW = "ANNOUNCEMENT_VIEW";

    // Chat
    public static final String CHAT_SEND = "CHAT_SEND";
    public static final String CHAT_VIEW = "CHAT_VIEW";

    // Meeting
    public static final String MEETING_CREATE = "MEETING_CREATE";
    public static final String MEETING_UPDATE = "MEETING_UPDATE";
    public static final String MEETING_JOIN = "MEETING_JOIN";
    public static final String MEETING_VIEW = "MEETING_VIEW";

    // Notification
    public static final String NOTIFICATION_VIEW = "NOTIFICATION_VIEW";

    // Dashboard / audit
    public static final String DASHBOARD_VIEW = "DASHBOARD_VIEW";
    public static final String AUDIT_VIEW = "AUDIT_VIEW";

    private Permissions() {
    }
}
