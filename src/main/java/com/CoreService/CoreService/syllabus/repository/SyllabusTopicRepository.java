package com.CoreService.CoreService.syllabus.repository;

import com.CoreService.CoreService.syllabus.dto.SyllabusProgressRow;
import com.CoreService.CoreService.syllabus.entity.SyllabusTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusTopicRepository extends JpaRepository<SyllabusTopic, UUID> {

    @Query("""
            select t from SyllabusTopic t
            join fetch t.unit u
            join fetch u.syllabus
            where t.id = :topicId and t.collegeId = :collegeId
            """)
    Optional<SyllabusTopic> findWithUnitByIdAndCollegeId(@Param("topicId") UUID topicId,
                                                         @Param("collegeId") UUID collegeId);

    /**
     * Every topic of a syllabus in one round trip; the unit is fetched so the
     * caller can group without touching a lazy reference per row.
     */
    @Query("""
            select t from SyllabusTopic t
            join fetch t.unit u
            where t.collegeId = :collegeId and u.syllabus.id = :syllabusId
            order by u.orderIndex asc nulls last, u.title asc, t.orderIndex asc nulls last, t.title asc
            """)
    List<SyllabusTopic> findBySyllabus(@Param("collegeId") UUID collegeId,
                                       @Param("syllabusId") UUID syllabusId);

    @Query("""
            select t from SyllabusTopic t
            where t.collegeId = :collegeId and t.unit.id = :unitId
            order by t.orderIndex asc nulls last, t.title asc
            """)
    List<SyllabusTopic> findByUnit(@Param("collegeId") UUID collegeId, @Param("unitId") UUID unitId);

    @Query("""
            select new com.CoreService.CoreService.syllabus.dto.SyllabusProgressRow(
                count(t.id),
                coalesce(sum(case when t.completed = true then 1L else 0L end), 0L))
            from SyllabusTopic t
            where t.collegeId = :collegeId and t.unit.syllabus.id = :syllabusId
            """)
    SyllabusProgressRow aggregateProgress(@Param("collegeId") UUID collegeId,
                                          @Param("syllabusId") UUID syllabusId);

    @Modifying
    @Query("delete from SyllabusTopic t where t.collegeId = :collegeId and t.unit.id = :unitId")
    void deleteByUnit(@Param("collegeId") UUID collegeId, @Param("unitId") UUID unitId);

    @Modifying
    @Query("""
            delete from SyllabusTopic t
            where t.collegeId = :collegeId
              and t.unit.id in (select u.id from SyllabusUnit u
                                where u.collegeId = :collegeId and u.syllabus.id = :syllabusId)
            """)
    void deleteBySyllabus(@Param("collegeId") UUID collegeId, @Param("syllabusId") UUID syllabusId);
}
