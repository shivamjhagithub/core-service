package com.CoreService.CoreService.Permission.Initializer;

import com.CoreService.CoreService.Permission.Entities.PermissionEntity;
import com.CoreService.CoreService.Permission.Repository.PermissionRepository;
import com.CoreService.CoreService.module.Entities.ModuleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionInitializer
        implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    @Override
    public void run(String... args) {
        Map<String, String> permissions = Map.ofEntries(

                // User
                Map.entry("CREATE_USER", "Create users"),
                Map.entry("UPDATE_USER", "Update users"),
                Map.entry("DELETE_USER", "Delete users"),
                Map.entry("VIEW_USER", "View users"),
                Map.entry("VIEW_ALL_USER", "view all users"),
                Map.entry("VIEW_ALL_COLLEGE_USER", "view all users of a college"),
                Map.entry("DELETE_ALL_COLLEGE_USER", "delete all users of a college"),
                // Role
                Map.entry("CREATE_ROLE", "Create roles"),
                Map.entry("UPDATE_ROLE", "Update roles"),
                Map.entry("DELETE_ROLE", "Delete roles"),
                Map.entry("VIEW_ROLE", "View roles"),
                Map.entry("ASSIGN_ROLE", "Assign roles to users"),
                Map.entry("REMOVE_ROLE", "Remove roles from users"),

                // Attendance
                Map.entry("MARK_ATTENDANCE", "Mark attendance"),
                Map.entry("EDIT_ATTENDANCE", "Edit attendance"),
                Map.entry("VIEW_ATTENDANCE", "View attendance"),
                Map.entry("DELETE_ATTENDANCE", "Delete attendance"),

                // Classes
                Map.entry("CREATE_CLASS", "Create classes"),
                Map.entry("UPDATE_CLASS", "Update classes"),
                Map.entry("DELETE_CLASS", "Delete classes"),
                Map.entry("VIEW_CLASS", "View classes"),
                Map.entry("TAKE_CLASS", "Conduct class"),

                // Timetable
                Map.entry("CREATE_TIMETABLE", "Create timetable"),
                Map.entry("UPDATE_TIMETABLE", "Update timetable"),
                Map.entry("DELETE_TIMETABLE", "Delete timetable"),
                Map.entry("VIEW_TIMETABLE", "View timetable"),

                // Exams
                Map.entry("CREATE_EXAM", "Create exams"),
                Map.entry("UPDATE_EXAM", "Update exams"),
                Map.entry("DELETE_EXAM", "Delete exams"),
                Map.entry("VIEW_EXAM", "View exams"),
                Map.entry("ENTER_MARKS", "Enter marks"),
                Map.entry("UPDATE_MARKS", "Update marks"),
                Map.entry("VIEW_MARKS", "View marks"),
                Map.entry("PUBLISH_RESULT", "Publish results"),

                // Assignments
                Map.entry("CREATE_ASSIGNMENT", "Create assignments"),
                Map.entry("UPDATE_ASSIGNMENT", "Update assignments"),
                Map.entry("DELETE_ASSIGNMENT", "Delete assignments"),
                Map.entry("VIEW_ASSIGNMENT", "View assignments"),
                Map.entry("SUBMIT_ASSIGNMENT", "Submit assignments"),
                Map.entry("GRADE_ASSIGNMENT", "Grade assignments"),

                // Notice
                Map.entry("CREATE_NOTICE", "Create notices"),
                Map.entry("UPDATE_NOTICE", "Update notices"),
                Map.entry("DELETE_NOTICE", "Delete notices"),
                Map.entry("VIEW_NOTICE", "View notices"),

                // Messaging
                Map.entry("SEND_MESSAGE", "Send messages"),
                Map.entry("VIEW_MESSAGE", "View messages"),
                Map.entry("DELETE_MESSAGE", "Delete messages"),

                // Events
                Map.entry("CREATE_EVENT", "Create events"),
                Map.entry("UPDATE_EVENT", "Update events"),
                Map.entry("DELETE_EVENT", "Delete events"),
                Map.entry("VIEW_EVENT", "View events"),

                // Library
                Map.entry("ADD_BOOK", "Add books"),
                Map.entry("UPDATE_BOOK", "Update books"),
                Map.entry("DELETE_BOOK", "Delete books"),
                Map.entry("VIEW_BOOK", "View books"),
                Map.entry("ISSUE_BOOK", "Issue books"),
                Map.entry("RETURN_BOOK", "Return books"),

                // Hostel
                Map.entry("ALLOCATE_ROOM", "Allocate hostel rooms"),
                Map.entry("UPDATE_ROOM", "Update hostel rooms"),
                Map.entry("VIEW_ROOM", "View hostel rooms"),
                Map.entry("VACATE_ROOM", "Vacate hostel rooms"),

                // Fees
                Map.entry("COLLECT_FEES", "Collect fees"),
                Map.entry("UPDATE_FEES", "Update fee records"),
                Map.entry("VIEW_FEES", "View fee records"),

                // Leave
                Map.entry("APPLY_LEAVE", "Apply leave"),
                Map.entry("APPROVE_LEAVE", "Approve leave"),
                Map.entry("REJECT_LEAVE", "Reject leave"),
                Map.entry("VIEW_LEAVE", "View leave requests"),

                // Reports
                Map.entry("VIEW_REPORT", "View reports"),
                Map.entry("EXPORT_REPORT", "Export reports"),

                // Profile
                Map.entry("VIEW_PROFILE", "View profile"),
                Map.entry("UPDATE_PROFILE", "Update profile"),

                // Settings
                Map.entry("VIEW_SETTINGS", "View settings"),
                Map.entry("UPDATE_SETTINGS", "Update settings"),

                //permissions
                Map.entry("GET_PERMISSION","can get permissions data"),
                Map.entry("ASSIGN_PERMISSION","permission can be assigned to role"),
                Map.entry("REMOVE_PERMISSION","permission can be deassigned for role")
        );


        for (Map.Entry<String, String> entry : permissions.entrySet()) {

            String permissionCode = entry.getKey();
            String description = entry.getValue();

            if (!permissionRepository.existsByPermissionCode(permissionCode)) {

                PermissionEntity permission = PermissionEntity.builder()
                        .permissionCode(permissionCode)
                        .permissionName(permissionCode.replace("_", " "))
                        .permissionDescription(description)
                        .build();

                permissionRepository.save(permission);
            }
        }
    }
}