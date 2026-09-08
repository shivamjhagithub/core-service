package com.CoreService.CoreService.announcement.repository;

import com.CoreService.CoreService.announcement.entity.Announcement;
import com.CoreService.CoreService.announcement.enums.AnnouncementTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Optional<Announcement> findByIdAndCollegeId(UUID id, UUID collegeId);

    @Query("""
            select a
              from Announcement a
             where a.collegeId = :collegeId
               and a.published = true
               and (a.target in :collegeWideTargets
                    or (a.target = :userTarget and a.targetUserId = :userId)
                    or (a.target = :classroomTarget and a.targetId in :classroomIds))
            """)
    Page<Announcement> findVisible(@Param("collegeId") UUID collegeId,
                                   @Param("userId") String userId,
                                   @Param("collegeWideTargets") Collection<AnnouncementTarget> collegeWideTargets,
                                   @Param("userTarget") AnnouncementTarget userTarget,
                                   @Param("classroomTarget") AnnouncementTarget classroomTarget,
                                   @Param("classroomIds") Collection<UUID> classroomIds,
                                   Pageable pageable);

    /**
     * Same feed for a caller who belongs to no classroom. It exists as its own
     * query because an empty {@code in ()} list is not valid SQL.
     */
    @Query("""
            select a
              from Announcement a
             where a.collegeId = :collegeId
               and a.published = true
               and (a.target in :collegeWideTargets
                    or (a.target = :userTarget and a.targetUserId = :userId))
            """)
    Page<Announcement> findVisibleWithoutClassrooms(
            @Param("collegeId") UUID collegeId,
            @Param("userId") String userId,
            @Param("collegeWideTargets") Collection<AnnouncementTarget> collegeWideTargets,
            @Param("userTarget") AnnouncementTarget userTarget,
            Pageable pageable);
}
