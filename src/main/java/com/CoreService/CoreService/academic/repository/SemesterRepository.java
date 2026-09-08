package com.CoreService.CoreService.academic.repository;

import com.CoreService.CoreService.academic.entity.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, UUID> {

    Optional<Semester> findByIdAndCollegeId(UUID id, UUID collegeId);

    Page<Semester> findAllByCollegeId(UUID collegeId, Pageable pageable);

    Page<Semester> findAllByCollegeIdAndProgram_Id(UUID collegeId, UUID programId, Pageable pageable);

    boolean existsByCollegeIdAndProgram_IdAndNumber(UUID collegeId, UUID programId, Integer number);

    boolean existsByCollegeIdAndProgram_IdAndNumberAndIdNot(UUID collegeId, UUID programId, Integer number, UUID id);

    boolean existsByCollegeIdAndProgram_Id(UUID collegeId, UUID programId);

    boolean existsByCollegeIdAndAcademicSession_Id(UUID collegeId, UUID academicSessionId);
}
