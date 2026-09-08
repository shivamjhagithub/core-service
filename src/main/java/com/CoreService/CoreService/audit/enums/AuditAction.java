package com.CoreService.CoreService.audit.enums;

/**
 * Action names written to the trail. Constants rather than an enum so a module
 * can record an action without the audit module having to be changed first,
 * while the names shared across modules still have one definition.
 */
public final class AuditAction {

    public static final String CLASSROOM_CREATED = "CLASSROOM_CREATED";
    public static final String CLASSROOM_MEMBER_ADDED = "CLASSROOM_MEMBER_ADDED";

    public static final String ATTENDANCE_MARKED = "ATTENDANCE_MARKED";

    public static final String ASSIGNMENT_CREATED = "ASSIGNMENT_CREATED";
    public static final String ASSIGNMENT_SUBMITTED = "ASSIGNMENT_SUBMITTED";
    public static final String ASSIGNMENT_GRADED = "ASSIGNMENT_GRADED";

    public static final String MATERIAL_UPLOADED = "MATERIAL_UPLOADED";
    public static final String SYLLABUS_TOPIC_COMPLETED = "SYLLABUS_TOPIC_COMPLETED";
    public static final String CHAT_MESSAGE_SENT = "CHAT_MESSAGE_SENT";

    public static final String MEETING_CREATED = "MEETING_CREATED";
    public static final String MEETING_STARTED = "MEETING_STARTED";

    public static final String ANNOUNCEMENT_CREATED = "ANNOUNCEMENT_CREATED";
    public static final String ANNOUNCEMENT_PUBLISHED = "ANNOUNCEMENT_PUBLISHED";
    public static final String ANNOUNCEMENT_DELETED = "ANNOUNCEMENT_DELETED";

    private AuditAction() {
    }
}
