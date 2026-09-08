package com.CoreService.CoreService.material.repository;

import com.CoreService.CoreService.material.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StudyMaterialRepository
        extends JpaRepository<StudyMaterial, UUID>, JpaSpecificationExecutor<StudyMaterial> {

    Optional<StudyMaterial> findByIdAndCollegeId(UUID id, UUID collegeId);
}
