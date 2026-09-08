package com.CoreService.CoreService.Permission.Initializer;

import com.CoreService.CoreService.Permission.Entities.PermissionEntity;
import com.CoreService.CoreService.Permission.Repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds the permission catalogue. Idempotent, so it can run on every start.
 * <p>
 * Two naming conventions coexist: the original {@code VERB_NOUN} codes used by
 * the user, role and college modules, and the {@code NOUN_VERB} codes used by
 * every module added since (mirroring
 * {@link com.CoreService.CoreService.common.security.Permissions}). New codes
 * should follow {@code NOUN_VERB}.
 */
@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissionInitializer.class);

    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {

        Map<String, String> permissions = new LinkedHashMap<>();

        // --- Platform / user administration (legacy VERB_NOUN codes) ---
        permissions.put("CREATE_USER", "Create users");
        permissions.put("UPDATE_USER", "Update users");
        permissions.put("DELETE_USER", "Delete users");
        permissions.put("VIEW_USER", "View users");
        permissions.put("VIEW_ALL_USER", "View all users");
        permissions.put("VIEW_ALL_COLLEGE_USER", "View all users of a college");
        permissions.put("DELETE_ALL_COLLEGE_USER", "Delete all users of a college");
        permissions.put("DELETE_ALL_COLLEGE_USERS", "Delete all users of a college");

        permissions.put("CREATE_ROLE", "Create roles");
        permissions.put("UPDATE_ROLE", "Update roles");
        permissions.put("DELETE_ROLE", "Delete roles");
        permissions.put("VIEW_ROLE", "View roles");
        permissions.put("ASSIGN_ROLE", "Assign roles to users");
        permissions.put("REMOVE_ROLE", "Remove roles from users");

        permissions.put("GET_PERMISSION", "Read the permission catalogue");
        permissions.put("ASSIGN_PERMISSION", "Grant a permission to a role");
        permissions.put("REMOVE_PERMISSION", "Revoke a permission from a role");

        permissions.put("VIEW_PROFILE", "View profile");
        permissions.put("UPDATE_PROFILE", "Update profile");
        permissions.put("VIEW_SETTINGS", "View college settings");
        permissions.put("UPDATE_SETTINGS", "Update college settings");
        permissions.put("VIEW_REPORT", "View reports");
        permissions.put("EXPORT_REPORT", "Export reports");

        // --- Academic structure ---
        permissions.put("ACADEMIC_MANAGE", "Manage departments, programs, sessions, semesters and subjects");
        permissions.put("ACADEMIC_VIEW", "View the academic structure");

        // --- Student and teacher records ---
        permissions.put("STUDENT_CREATE", "Create student records");
        permissions.put("STUDENT_UPDATE", "Update student records");
        permissions.put("STUDENT_VIEW", "View student records");
        permissions.put("TEACHER_CREATE", "Create teacher records");
        permissions.put("TEACHER_UPDATE", "Update teacher records");
        permissions.put("TEACHER_VIEW", "View teacher records");

        // --- Classroom ---
        permissions.put("CLASSROOM_CREATE", "Create classrooms");
        permissions.put("CLASSROOM_UPDATE", "Update classrooms");
        permissions.put("CLASSROOM_DELETE", "Delete classrooms");
        permissions.put("CLASSROOM_VIEW", "View classrooms");
        permissions.put("CLASSROOM_MEMBER_MANAGE", "Add or remove classroom members");
        permissions.put("CLASSROOM_PERMISSION_MANAGE", "Configure classroom permissions");

        // --- Attendance ---
        permissions.put("ATTENDANCE_CREATE", "Create attendance sessions and mark attendance");
        permissions.put("ATTENDANCE_UPDATE", "Update attendance records");
        permissions.put("ATTENDANCE_VIEW", "View attendance and reports");

        // --- Syllabus ---
        permissions.put("SYLLABUS_MANAGE", "Manage syllabi, units and topics");
        permissions.put("SYLLABUS_VIEW", "View syllabi and progress");

        // --- Assignment ---
        permissions.put("ASSIGNMENT_CREATE", "Create assignments");
        permissions.put("ASSIGNMENT_UPDATE", "Update assignments");
        permissions.put("ASSIGNMENT_DELETE", "Delete assignments");
        permissions.put("ASSIGNMENT_VIEW", "View assignments");
        permissions.put("ASSIGNMENT_SUBMIT", "Submit assignments");
        permissions.put("ASSIGNMENT_GRADE", "Grade assignment submissions");

        // --- Study material ---
        permissions.put("MATERIAL_UPLOAD", "Upload study material");
        permissions.put("MATERIAL_DELETE", "Delete study material");
        permissions.put("MATERIAL_VIEW", "View study material");

        // --- Announcement ---
        permissions.put("ANNOUNCEMENT_CREATE", "Create announcements");
        permissions.put("ANNOUNCEMENT_DELETE", "Delete announcements");
        permissions.put("ANNOUNCEMENT_VIEW", "View announcements");

        // --- Chat ---
        permissions.put("CHAT_SEND", "Send chat messages");
        permissions.put("CHAT_VIEW", "Read chat history");

        // --- Video conference ---
        permissions.put("MEETING_CREATE", "Schedule meetings");
        permissions.put("MEETING_UPDATE", "Update, start, end or cancel meetings");
        permissions.put("MEETING_JOIN", "Join meetings");
        permissions.put("MEETING_VIEW", "View meetings");

        // --- Notification / dashboard / audit ---
        permissions.put("NOTIFICATION_VIEW", "View notifications");
        permissions.put("DASHBOARD_VIEW", "View dashboards");
        permissions.put("AUDIT_VIEW", "View audit logs");

        int created = 0;
        for (Map.Entry<String, String> entry : permissions.entrySet()) {

            String permissionCode = entry.getKey();

            if (Boolean.TRUE.equals(permissionRepository.existsByPermissionCode(permissionCode))) {
                continue;
            }

            permissionRepository.save(PermissionEntity.builder()
                    .permissionCode(permissionCode)
                    .permissionName(permissionCode.replace('_', ' '))
                    .permissionDescription(entry.getValue())
                    .build());
            created++;
        }

        if (created > 0) {
            log.info("Seeded {} new permissions", created);
        }
    }
}
