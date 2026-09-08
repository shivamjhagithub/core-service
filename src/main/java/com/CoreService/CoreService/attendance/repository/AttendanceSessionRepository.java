package com.CoreService.CoreService.attendance.repository;

import com.CoreService.CoreService.attendance.entity.AttendanceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    Optional<AttendanceSession> findByIdAndCollegeId(UUID id, UUID collegeId);

    Page<AttendanceSession> findAllByCollegeIdAndClassroomId(UUID collegeId, UUID classroomId, Pageable pageable);

    boolean existsByCollegeIdAndClassroomIdAndSessionDateAndSubjectId(UUID collegeId,
                                                                     UUID classroomId,
                                                                     LocalDate sessionDate,
                                                                     UUID subjectId);

    /**
     * MySQL treats NULLs as distinct, so the unique index does not catch a
     * second subject-less session for the same day; this check does.
     */
    boolean existsByCollegeIdAndClassroomIdAndSessionDateAndSubjectIdIsNull(UUID collegeId,
                                                                           UUID classroomId,
                                                                           LocalDate sessionDate);

    long countByCollegeIdAndClassroomId(UUID collegeId, UUID classroomId);
}
