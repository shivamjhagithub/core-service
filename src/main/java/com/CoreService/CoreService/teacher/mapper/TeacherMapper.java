package com.CoreService.CoreService.teacher.mapper;

import com.CoreService.CoreService.teacher.dto.TeacherResponse;
import com.CoreService.CoreService.teacher.dto.TeacherSubjectResponse;
import com.CoreService.CoreService.teacher.entity.Teacher;
import com.CoreService.CoreService.teacher.entity.TeacherSubject;
import org.springframework.stereotype.Component;

@Component
public class TeacherMapper {

    public TeacherResponse toTeacherResponse(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getUserId(),
                teacher.getEmployeeId(),
                teacher.getDesignation(),
                teacher.getQualification(),
                teacher.getDepartmentId(),
                teacher.isActive(),
                teacher.getCreatedAt(),
                teacher.getUpdatedAt());
    }

    /** Reading the id off the lazy teacher proxy does not initialise it. */
    public TeacherSubjectResponse toTeacherSubjectResponse(TeacherSubject teacherSubject) {
        return new TeacherSubjectResponse(
                teacherSubject.getId(),
                teacherSubject.getTeacher().getId(),
                teacherSubject.getSubjectId(),
                teacherSubject.getCreatedAt());
    }
}
