package com.CoreService.CoreService.assignment.repository;

import com.CoreService.CoreService.assignment.entity.AssignmentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    /**
     * Loads the submission with its assignment, which carries the classroom the
     * caller has to be authorized against and the marks ceiling.
     */
    @Query("""
            select s from AssignmentSubmission s
            join fetch s.assignment
            where s.id = :submissionId and s.collegeId = :collegeId
            """)
    Optional<AssignmentSubmission> findWithAssignmentByIdAndCollegeId(@Param("submissionId") UUID submissionId,
                                                                      @Param("collegeId") UUID collegeId);

    Optional<AssignmentSubmission> findByCollegeIdAndAssignment_IdAndStudentUserId(UUID collegeId,
                                                                                   UUID assignmentId,
                                                                                   String studentUserId);

    boolean existsByCollegeIdAndAssignment_IdAndStudentUserId(UUID collegeId,
                                                              UUID assignmentId,
                                                              String studentUserId);

    Page<AssignmentSubmission> findByCollegeIdAndAssignment_Id(UUID collegeId, UUID assignmentId, Pageable pageable);

    long countByCollegeIdAndAssignment_Id(UUID collegeId, UUID assignmentId);

    @Query(value = """
            select s from AssignmentSubmission s
            join fetch s.assignment
            where s.collegeId = :collegeId and s.studentUserId = :studentUserId
            """,
            countQuery = """
            select count(s) from AssignmentSubmission s
            where s.collegeId = :collegeId and s.studentUserId = :studentUserId
            """)
    Page<AssignmentSubmission> findByStudent(@Param("collegeId") UUID collegeId,
                                             @Param("studentUserId") String studentUserId,
                                             Pageable pageable);
}
