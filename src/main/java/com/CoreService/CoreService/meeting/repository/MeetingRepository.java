package com.CoreService.CoreService.meeting.repository;

import com.CoreService.CoreService.meeting.entity.Meeting;
import com.CoreService.CoreService.meeting.enums.MeetingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    Optional<Meeting> findByIdAndCollegeId(UUID id, UUID collegeId);

    Page<Meeting> findByCollegeIdAndClassroomId(UUID collegeId, UUID classroomId, Pageable pageable);

    @Query("""
            select m from Meeting m
            where m.collegeId = :collegeId
              and m.hostUserId = :userId
              and m.status in :statuses
              and m.scheduledStartTime >= :from
            order by m.scheduledStartTime asc
            """)
    List<Meeting> findUpcomingHostedBy(@Param("collegeId") UUID collegeId,
                                       @Param("userId") String userId,
                                       @Param("statuses") Collection<MeetingStatus> statuses,
                                       @Param("from") Instant from);

    /**
     * Kept separate from the hosted query rather than folded into one {@code or}
     * so it is only issued when the caller actually belongs to a classroom; an
     * empty {@code in} list has no portable meaning.
     */
    @Query("""
            select m from Meeting m
            where m.collegeId = :collegeId
              and m.classroomId in :classroomIds
              and m.status in :statuses
              and m.scheduledStartTime >= :from
            order by m.scheduledStartTime asc
            """)
    List<Meeting> findUpcomingForClassrooms(@Param("collegeId") UUID collegeId,
                                            @Param("classroomIds") Collection<UUID> classroomIds,
                                            @Param("statuses") Collection<MeetingStatus> statuses,
                                            @Param("from") Instant from);
}
