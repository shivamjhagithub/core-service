package com.CoreService.CoreService.syllabus.repository;

import com.CoreService.CoreService.syllabus.entity.Syllabus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SyllabusRepository extends JpaRepository<Syllabus, UUID>, JpaSpecificationExecutor<Syllabus> {

    Optional<Syllabus> findByIdAndCollegeId(UUID id, UUID collegeId);
}
