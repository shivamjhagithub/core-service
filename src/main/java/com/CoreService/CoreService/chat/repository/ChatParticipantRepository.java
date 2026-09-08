package com.CoreService.CoreService.chat.repository;

import com.CoreService.CoreService.chat.dto.ChatUnreadCountResponse;
import com.CoreService.CoreService.chat.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {

    /**
     * Membership probe used by both the message path and STOMP subscription
     * authorization, so it must never depend on request-scoped context.
     */
    @Query("""
            select case when count(p) > 0 then true else false end
            from ChatParticipant p
            where p.collegeId = :collegeId
              and p.chatRoom.id = :chatRoomId
              and p.userId = :userId
            """)
    boolean isParticipant(@Param("collegeId") UUID collegeId,
                          @Param("chatRoomId") UUID chatRoomId,
                          @Param("userId") String userId);

    @Query("""
            select p.userId from ChatParticipant p
            where p.collegeId = :collegeId and p.chatRoom.id = :chatRoomId
            """)
    List<String> findUserIdsByRoom(@Param("collegeId") UUID collegeId,
                                   @Param("chatRoomId") UUID chatRoomId);

    /**
     * Unread messages per room for one participant, resolved in a single
     * aggregate so opening the room list never degrades into a query per room.
     * Rooms with nothing unread are still reported, with a count of zero.
     */
    @Query("""
            select new com.CoreService.CoreService.chat.dto.ChatUnreadCountResponse(
                       p.chatRoom.id, count(m.id))
            from ChatParticipant p
                left join ChatMessage m
                    on m.chatRoom = p.chatRoom
                    and m.collegeId = p.collegeId
                    and m.deleted = false
                    and m.senderUserId <> p.userId
                    and (p.lastReadAt is null or m.createdAt > p.lastReadAt)
            where p.collegeId = :collegeId and p.userId = :userId
            group by p.chatRoom.id
            """)
    List<ChatUnreadCountResponse> unreadCounts(@Param("collegeId") UUID collegeId,
                                               @Param("userId") String userId);

    @Modifying
    @Query("""
            update ChatParticipant p set p.lastReadAt = :readAt
            where p.collegeId = :collegeId
              and p.chatRoom.id = :chatRoomId
              and p.userId = :userId
            """)
    int markRead(@Param("collegeId") UUID collegeId,
                 @Param("chatRoomId") UUID chatRoomId,
                 @Param("userId") String userId,
                 @Param("readAt") LocalDateTime readAt);

    /**
     * Drops participants who are no longer classroom members; the subscription
     * rule authorizes on these rows, so stale ones would keep granting access.
     */
    @Modifying
    @Query("""
            delete from ChatParticipant p
            where p.collegeId = :collegeId
              and p.chatRoom.id = :chatRoomId
              and p.userId in :userIds
            """)
    int deleteByRoomAndUserIds(@Param("collegeId") UUID collegeId,
                               @Param("chatRoomId") UUID chatRoomId,
                               @Param("userIds") Collection<String> userIds);
}
