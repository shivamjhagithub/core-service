package com.CoreService.CoreService.chat.repository;

import com.CoreService.CoreService.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is scoped by college: a room belonging to another tenant must be
 * indistinguishable from a room that does not exist.
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByIdAndCollegeId(UUID id, UUID collegeId);

    Optional<ChatRoom> findByCollegeIdAndDirectKey(UUID collegeId, String directKey);

    Optional<ChatRoom> findByCollegeIdAndClassroomId(UUID collegeId, UUID classroomId);
}
