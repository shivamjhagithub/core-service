package com.CoreService.CoreService.academic.repository;

import com.CoreService.CoreService.academic.entity.AcademicSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicSessionRepository extends JpaRepository<AcademicSession, UUID> {

    Optional<AcademicSession> findByIdAndCollegeId(UUID id, UUID collegeId);

    Page<AcademicSession> findAllByCollegeId(UUID collegeId, Pageable pageable);

    Optional<AcademicSession> findByCollegeIdAndCurrentTrue(UUID collegeId);

    /**
     * Returns a list rather than a single row on purpose: it is the input of the
     * routine that repairs the "one current session per college" invariant.
     */
    List<AcademicSession> findAllByCollegeIdAndCurrentTrue(UUID collegeId);

    boolean existsByCollegeIdAndNameIgnoreCase(UUID collegeId, String name);

    boolean existsByCollegeIdAndNameIgnoreCaseAndIdNot(UUID collegeId, String name, UUID id);
}
