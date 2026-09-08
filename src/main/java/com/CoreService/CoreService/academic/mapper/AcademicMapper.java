package com.CoreService.CoreService.academic.mapper;

import com.CoreService.CoreService.academic.dto.AcademicSessionResponse;
import com.CoreService.CoreService.academic.dto.DepartmentResponse;
import com.CoreService.CoreService.academic.dto.ProgramResponse;
import com.CoreService.CoreService.academic.dto.SemesterResponse;
import com.CoreService.CoreService.academic.dto.SubjectResponse;
import com.CoreService.CoreService.academic.entity.AcademicSession;
import com.CoreService.CoreService.academic.entity.Department;
import com.CoreService.CoreService.academic.entity.Program;
import com.CoreService.CoreService.academic.entity.Semester;
import com.CoreService.CoreService.academic.entity.Subject;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Entity to DTO translation for the academic module. Only parent identifiers are
 * exposed, which also keeps these methods free of lazy-association loading:
 * reading the id off a Hibernate proxy does not initialise it.
 */
@Component
public class AcademicMapper {

    public DepartmentResponse toDepartmentResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                department.getDescription(),
                department.getCreatedAt(),
                department.getUpdatedAt());
    }

    public ProgramResponse toProgramResponse(Program program) {
        return new ProgramResponse(
                program.getId(),
                program.getName(),
                program.getCode(),
                program.getDurationYears(),
                program.getDescription(),
                program.getDepartment().getId(),
                program.getCreatedAt(),
                program.getUpdatedAt());
    }

    public AcademicSessionResponse toSessionResponse(AcademicSession session) {
        return new AcademicSessionResponse(
                session.getId(),
                session.getName(),
                session.getStartDate(),
                session.getEndDate(),
                session.isCurrent(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    public SemesterResponse toSemesterResponse(Semester semester) {
        AcademicSession session = semester.getAcademicSession();
        UUID sessionId = session == null ? null : session.getId();

        return new SemesterResponse(
                semester.getId(),
                semester.getNumber(),
                semester.getName(),
                semester.getProgram().getId(),
                sessionId,
                semester.getCreatedAt(),
                semester.getUpdatedAt());
    }

    public SubjectResponse toSubjectResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                subject.getCredits(),
                subject.getDescription(),
                subject.getSemester().getId(),
                subject.getCreatedAt(),
                subject.getUpdatedAt());
    }
}
