package com.CoreService.CoreService.file.repository;

import com.CoreService.CoreService.file.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    Optional<StoredFile> findByIdAndCollegeId(UUID id, UUID collegeId);

    List<StoredFile> findByIdInAndCollegeId(Collection<UUID> ids, UUID collegeId);
}
