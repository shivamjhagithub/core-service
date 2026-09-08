package com.CoreService.CoreService.teacher.repository;

import com.CoreService.CoreService.teacher.entity.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, UUID> {

    Optional<TeacherSubject> findByCollegeIdAndTeacher_IdAndSubjectId(
            UUID collegeId, UUID teacherId, UUID subjectId);

    boolean existsByCollegeIdAndTeacher_IdAndSubjectId(UUID collegeId, UUID teacherId, UUID subjectId);

    List<TeacherSubject> findAllByCollegeIdAndTeacher_Id(UUID collegeId, UUID teacherId);
}
