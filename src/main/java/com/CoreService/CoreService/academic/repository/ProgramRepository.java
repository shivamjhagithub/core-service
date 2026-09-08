package com.CoreService.CoreService.academic.repository;

import com.CoreService.CoreService.academic.entity.Program;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {

    Optional<Program> findByIdAndCollegeId(UUID id, UUID collegeId);

    Page<Program> findAllByCollegeId(UUID collegeId, Pageable pageable);

    Page<Program> findAllByCollegeIdAndDepartment_Id(UUID collegeId, UUID departmentId, Pageable pageable);

    boolean existsByCollegeIdAndCodeIgnoreCase(UUID collegeId, String code);

    boolean existsByCollegeIdAndCodeIgnoreCaseAndIdNot(UUID collegeId, String code, UUID id);

    boolean existsByCollegeIdAndDepartment_Id(UUID collegeId, UUID departmentId);
}
