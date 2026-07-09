package com.CoreService.CoreService.module.Initializer;

import com.CoreService.CoreService.module.Entities.ModuleEntity;
import com.CoreService.CoreService.module.Repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ModuleDataInitializer
        implements CommandLineRunner {

    private final ModuleRepository
            moduleRepository;

    @Override
    public void run(String... args) {
        Map<String, String> modules = Map.of(

                "ATTENDANCE",
                "Manages student attendance records and tracking",

                "LIBRARY",
                "Controls library books, issuing, returns, and fines",

                "HOSTEL",
                "Manages hostel rooms, allotments, and resident details",

                "EXAM",
                "Handles examinations, results, marks, and grading",

                "ADMISSION",
                "Manages student admissions and enrollment process",

                "TIMETABLE",
                "Handles timetable scheduling and class management",

                "FACULTY",
                "Manages faculty details, departments, and assignments",

                "NOTICE",
                "Manages notices, announcements, and notifications",

                "EVENT",
                "Handles events, seminars, and college activities"

                );
        for (Map.Entry<String, String> entry
                : modules.entrySet()) {

            String moduleCode =
                    entry.getKey();

            String description =
                    entry.getValue();

            boolean exists =
                    moduleRepository
                            .existsByModuleCode(
                                    moduleCode
                            );

            if (!exists) {

                ModuleEntity module =

                        ModuleEntity
                                .builder()

                                .moduleCode(
                                        moduleCode
                                )

                                .moduleName(
                                        moduleCode +
                                                " Module"
                                )

                                .moduleDescription(
                                        description
                                )

                                .build();

                moduleRepository.save(
                        module
                );
            }
        }
    }
}