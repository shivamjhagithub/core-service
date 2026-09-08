package com.CoreService.CoreService.module.Initializer;

import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.module.Entities.ModuleEntity;
import com.CoreService.CoreService.module.Repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds the catalogue of ERP modules a college can switch on. Idempotent.
 */
@Component
@RequiredArgsConstructor
@Order(0)
public class ModuleDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ModuleDataInitializer.class);

    private final ModuleRepository moduleRepository;

    @Override
    public void run(String... args) {

        Map<String, String> modules = new LinkedHashMap<>();

        modules.put(ModuleCodes.USER_MANAGEMENT, "Users, students, teachers and their records");
        modules.put(ModuleCodes.ACADEMIC, "Departments, programs, sessions, semesters and subjects");
        modules.put(ModuleCodes.CLASSROOM, "Classrooms, membership and classroom-level permissions");
        modules.put(ModuleCodes.ATTENDANCE, "Attendance sessions, records and reports");
        modules.put(ModuleCodes.SYLLABUS, "Syllabus structure and completion tracking");
        modules.put(ModuleCodes.ASSIGNMENT, "Assignments, submissions and grading");
        modules.put(ModuleCodes.STUDY_MATERIAL, "Study material distribution");
        modules.put(ModuleCodes.ANNOUNCEMENT, "Announcements to colleges, departments and classrooms");
        modules.put(ModuleCodes.CHAT, "Direct and classroom group chat");
        modules.put(ModuleCodes.VIDEO_CONFERENCE, "Meeting scheduling, participants and signaling");
        modules.put(ModuleCodes.NOTIFICATION, "In-app, WebSocket and push notifications");

        // Modules from the original ERP catalogue, kept so existing
        // college_modules rows keep resolving.
        modules.put("LIBRARY", "Library books, issuing, returns and fines");
        modules.put("HOSTEL", "Hostel rooms, allotments and resident details");
        modules.put("EXAM", "Examinations, results, marks and grading");
        modules.put("ADMISSION", "Student admissions and enrollment");
        modules.put("TIMETABLE", "Timetable scheduling");
        modules.put("FACULTY", "Faculty details, departments and assignments");
        modules.put("NOTICE", "Notices and circulars");
        modules.put("EVENT", "Events, seminars and college activities");

        int created = 0;
        for (Map.Entry<String, String> entry : modules.entrySet()) {

            String moduleCode = entry.getKey();

            if (Boolean.TRUE.equals(moduleRepository.existsByModuleCode(moduleCode))) {
                continue;
            }

            moduleRepository.save(ModuleEntity.builder()
                    .moduleCode(moduleCode)
                    .moduleName(moduleCode.replace('_', ' ') + " Module")
                    .moduleDescription(entry.getValue())
                    .build());
            created++;
        }

        if (created > 0) {
            log.info("Seeded {} new ERP modules", created);
        }
    }
}
