package com.CoreService.CoreService.academic.repository;

import com.CoreService.CoreService.academic.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is college-scoped: a department of another college must look
 * like it does not exist.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByIdAndCollegeId(UUID id, UUID collegeId);

    Page<Department> findAllByCollegeId(UUID collegeId, Pageable pageable);

    boolean existsByCollegeIdAndCodeIgnoreCase(UUID collegeId, String code);

    boolean existsByCollegeIdAndCodeIgnoreCaseAndIdNot(UUID collegeId, String code, UUID id);
}
