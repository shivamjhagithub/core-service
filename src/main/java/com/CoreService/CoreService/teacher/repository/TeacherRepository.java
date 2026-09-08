package com.CoreService.CoreService.teacher.repository;

import com.CoreService.CoreService.teacher.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    Optional<Teacher> findByIdAndCollegeId(UUID id, UUID collegeId);

    Optional<Teacher> findByCollegeIdAndUserId(UUID collegeId, String userId);

    boolean existsByCollegeIdAndUserId(UUID collegeId, String userId);

    Page<Teacher> findAllByCollegeId(UUID collegeId, Pageable pageable);

    boolean existsByCollegeIdAndEmployeeIdIgnoreCase(UUID collegeId, String employeeId);

    boolean existsByCollegeIdAndEmployeeIdIgnoreCaseAndIdNot(UUID collegeId, String employeeId, UUID id);

    /**
     * Keyword search over employee id, designation and user id. The college
     * filter is part of the query itself so a caller cannot widen it.
     */
    @Query("""
            select t from Teacher t
            where t.collegeId = :collegeId
              and (lower(coalesce(t.employeeId, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(t.designation, '')) like lower(concat('%', :keyword, '%'))
                or lower(t.userId) like lower(concat('%', :keyword, '%')))
            """)
    Page<Teacher> search(@Param("collegeId") UUID collegeId,
                         @Param("keyword") String keyword,
                         Pageable pageable);
}
