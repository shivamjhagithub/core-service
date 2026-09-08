package com.CoreService.CoreService.chat.repository;

import com.CoreService.CoreService.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * The room is fetched with the page because every message is mapped with its
     * room id, and a lazy proxy per row would turn one page into N queries.
     */
    @Query(value = """
            select m from ChatMessage m
                join fetch m.chatRoom
            where m.collegeId = :collegeId
              and m.chatRoom.id = :chatRoomId
              and m.deleted = false
            """,
            countQuery = """
                    select count(m) from ChatMessage m
                    where m.collegeId = :collegeId
                      and m.chatRoom.id = :chatRoomId
                      and m.deleted = false
                    """)
    Page<ChatMessage> findHistory(@Param("collegeId") UUID collegeId,
                                  @Param("chatRoomId") UUID chatRoomId,
                                  Pageable pageable);

    @Query("""
            select m from ChatMessage m
                join fetch m.chatRoom
            where m.id = :id and m.collegeId = :collegeId
            """)
    Optional<ChatMessage> findByIdAndCollegeId(@Param("id") UUID id,
                                               @Param("collegeId") UUID collegeId);
}
