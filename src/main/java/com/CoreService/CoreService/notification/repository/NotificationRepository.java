package com.CoreService.CoreService.notification.repository;

import com.CoreService.CoreService.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByCollegeIdAndRecipientUserId(UUID collegeId, String recipientUserId, Pageable pageable);

    Page<Notification> findByCollegeIdAndRecipientUserIdAndReadFalse(UUID collegeId,
                                                                    String recipientUserId,
                                                                    Pageable pageable);

    long countByCollegeIdAndRecipientUserIdAndReadFalse(UUID collegeId, String recipientUserId);

    Optional<Notification> findByIdAndCollegeId(UUID id, UUID collegeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
               set n.read = true,
                   n.readAt = :readAt
             where n.collegeId = :collegeId
               and n.recipientUserId = :recipientUserId
               and n.read = false
            """)
    int markAllRead(@Param("collegeId") UUID collegeId,
                    @Param("recipientUserId") String recipientUserId,
                    @Param("readAt") LocalDateTime readAt);
}
