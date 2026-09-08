package com.CoreService.CoreService.student.mapper;

import com.CoreService.CoreService.student.dto.StudentResponse;
import com.CoreService.CoreService.student.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getUserId(),
                student.getRollNumber(),
                student.getRegistrationNumber(),
                student.getFatherName(),
                student.getBloodGroup(),
                student.getSemester(),
                student.getDepartmentId(),
                student.getProgramId(),
                student.isActive(),
                student.getCreatedAt(),
                student.getUpdatedAt());
    }
}
