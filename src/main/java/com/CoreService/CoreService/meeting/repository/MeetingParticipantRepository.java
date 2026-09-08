package com.CoreService.CoreService.meeting.repository;

import com.CoreService.CoreService.meeting.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {

    /**
     * Roster membership, used by STOMP subscription authorization. Context-free
     * on purpose: it runs before any request context exists.
     */
    @Query("""
            select case when count(p) > 0 then true else false end
            from MeetingParticipant p
            where p.collegeId = :collegeId
              and p.meeting.id = :meetingId
              and p.userId = :userId
            """)
    boolean isParticipant(@Param("collegeId") UUID collegeId,
                          @Param("meetingId") UUID meetingId,
                          @Param("userId") String userId);

    /**
     * Presence in the call right now, which is what signaling relay requires:
     * someone who has left must not be able to keep pushing offers at peers.
     */
    @Query("""
            select case when count(p) > 0 then true else false end
            from MeetingParticipant p
            where p.collegeId = :collegeId
              and p.meeting.id = :meetingId
              and p.userId = :userId
              and p.leftAt is null
            """)
    boolean isActiveParticipant(@Param("collegeId") UUID collegeId,
                                @Param("meetingId") UUID meetingId,
                                @Param("userId") String userId);

    @Query("""
            select p from MeetingParticipant p
            where p.collegeId = :collegeId
              and p.meeting.id = :meetingId
              and p.userId = :userId
            """)
    Optional<MeetingParticipant> findParticipant(@Param("collegeId") UUID collegeId,
                                                 @Param("meetingId") UUID meetingId,
                                                 @Param("userId") String userId);

    @Query("""
            select p from MeetingParticipant p
            where p.collegeId = :collegeId and p.meeting.id = :meetingId
            order by p.joinedAt asc, p.userId asc
            """)
    List<MeetingParticipant> findRoster(@Param("collegeId") UUID collegeId,
                                        @Param("meetingId") UUID meetingId);

    /** Closes out everyone still marked present when a meeting ends. */
    @Modifying
    @Query("""
            update MeetingParticipant p set p.leftAt = :leftAt
            where p.collegeId = :collegeId
              and p.meeting.id = :meetingId
              and p.leftAt is null
            """)
    int markAllLeft(@Param("collegeId") UUID collegeId,
                    @Param("meetingId") UUID meetingId,
                    @Param("leftAt") Instant leftAt);
}
