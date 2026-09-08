package com.CoreService.CoreService.academic.repository;

import com.CoreService.CoreService.academic.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    Optional<Subject> findByIdAndCollegeId(UUID id, UUID collegeId);

    Page<Subject> findAllByCollegeId(UUID collegeId, Pageable pageable);

    Page<Subject> findAllByCollegeIdAndSemester_Id(UUID collegeId, UUID semesterId, Pageable pageable);

    boolean existsByIdAndCollegeId(UUID id, UUID collegeId);

    boolean existsByCollegeIdAndCodeIgnoreCase(UUID collegeId, String code);

    boolean existsByCollegeIdAndCodeIgnoreCaseAndIdNot(UUID collegeId, String code, UUID id);

    boolean existsByCollegeIdAndSemester_Id(UUID collegeId, UUID semesterId);
}
