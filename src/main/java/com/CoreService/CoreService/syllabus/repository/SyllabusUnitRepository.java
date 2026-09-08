package com.CoreService.CoreService.syllabus.repository;

import com.CoreService.CoreService.syllabus.entity.SyllabusUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusUnitRepository extends JpaRepository<SyllabusUnit, UUID> {

    /**
     * Loads the unit together with its syllabus, which carries the tenant and
     * classroom scope every write has to authorize against.
     */
    @Query("""
            select u from SyllabusUnit u
            join fetch u.syllabus
            where u.id = :unitId and u.collegeId = :collegeId
            """)
    Optional<SyllabusUnit> findWithSyllabusByIdAndCollegeId(@Param("unitId") UUID unitId,
                                                            @Param("collegeId") UUID collegeId);

    @Query("""
            select u from SyllabusUnit u
            where u.collegeId = :collegeId and u.syllabus.id = :syllabusId
            order by u.orderIndex asc nulls last, u.title asc
            """)
    List<SyllabusUnit> findBySyllabus(@Param("collegeId") UUID collegeId,
                                      @Param("syllabusId") UUID syllabusId);

    @Modifying
    @Query("delete from SyllabusUnit u where u.collegeId = :collegeId and u.syllabus.id = :syllabusId")
    void deleteBySyllabus(@Param("collegeId") UUID collegeId, @Param("syllabusId") UUID syllabusId);
}
