package com.CoreService.CoreService.classroom.repository;

import com.CoreService.CoreService.classroom.entity.Classroom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

    Optional<Classroom> findByIdAndCollegeId(UUID id, UUID collegeId);

    Page<Classroom> findAllByCollegeIdAndArchived(UUID collegeId, boolean archived, Pageable pageable);

    @Query(value = """
            select c from Classroom c
            where c.collegeId = :collegeId
              and c.archived = :archived
              and c.id in (select m.classroom.id from ClassroomMember m
                           where m.collegeId = :collegeId
                             and m.userId = :userId)
            """,
            countQuery = """
            select count(c) from Classroom c
            where c.collegeId = :collegeId
              and c.archived = :archived
              and c.id in (select m.classroom.id from ClassroomMember m
                           where m.collegeId = :collegeId
                             and m.userId = :userId)
            """)
    Page<Classroom> findMemberClassrooms(@Param("collegeId") UUID collegeId,
                                        @Param("userId") String userId,
                                        @Param("archived") boolean archived,
                                        Pageable pageable);
}
