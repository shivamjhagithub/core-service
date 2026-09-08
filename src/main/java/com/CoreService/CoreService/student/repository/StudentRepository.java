package com.CoreService.CoreService.student.repository;

import com.CoreService.CoreService.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndCollegeId(UUID id, UUID collegeId);

    Optional<Student> findByCollegeIdAndUserId(UUID collegeId, String userId);

    boolean existsByCollegeIdAndUserId(UUID collegeId, String userId);

    Page<Student> findAllByCollegeId(UUID collegeId, Pageable pageable);

    boolean existsByCollegeIdAndRollNumberIgnoreCase(UUID collegeId, String rollNumber);

    boolean existsByCollegeIdAndRollNumberIgnoreCaseAndIdNot(UUID collegeId, String rollNumber, UUID id);

    boolean existsByCollegeIdAndRegistrationNumberIgnoreCase(UUID collegeId, String registrationNumber);

    boolean existsByCollegeIdAndRegistrationNumberIgnoreCaseAndIdNot(
            UUID collegeId, String registrationNumber, UUID id);

    /**
     * Keyword search over roll number, registration number and user id. The
     * college filter is part of the query itself so a caller cannot widen it.
     */
    @Query("""
            select s from Student s
            where s.collegeId = :collegeId
              and (lower(coalesce(s.rollNumber, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(s.registrationNumber, '')) like lower(concat('%', :keyword, '%'))
                or lower(s.userId) like lower(concat('%', :keyword, '%')))
            """)
    Page<Student> search(@Param("collegeId") UUID collegeId,
                         @Param("keyword") String keyword,
                         Pageable pageable);
}
