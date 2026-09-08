package com.CoreService.CoreService.classroom.repository;

import com.CoreService.CoreService.classroom.dto.ClassroomMemberCount;
import com.CoreService.CoreService.classroom.entity.ClassroomMember;
import com.CoreService.CoreService.classroom.enums.ClassroomMemberType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, UUID> {

    boolean existsByCollegeIdAndClassroom_IdAndUserId(UUID collegeId, UUID classroomId, String userId);

    boolean existsByCollegeIdAndClassroom_IdAndUserIdAndMemberType(UUID collegeId,
                                                                  UUID classroomId,
                                                                  String userId,
                                                                  ClassroomMemberType memberType);

    Optional<ClassroomMember> findByCollegeIdAndClassroom_IdAndUserId(UUID collegeId,
                                                                     UUID classroomId,
                                                                     String userId);

    List<ClassroomMember> findAllByCollegeIdAndClassroom_IdOrderByJoinedAtAsc(UUID collegeId, UUID classroomId);

    List<ClassroomMember> findAllByCollegeIdAndClassroom_IdAndUserIdIn(UUID collegeId,
                                                                       UUID classroomId,
                                                                       Collection<String> userIds);

    long countByCollegeIdAndClassroom_IdAndMemberType(UUID collegeId,
                                                      UUID classroomId,
                                                      ClassroomMemberType memberType);

    @Query("""
            select m.userId from ClassroomMember m
            where m.collegeId = :collegeId and m.classroom.id = :classroomId
            order by m.userId
            """)
    List<String> findMemberUserIds(@Param("collegeId") UUID collegeId,
                                   @Param("classroomId") UUID classroomId);

    @Query("""
            select m.userId from ClassroomMember m
            where m.collegeId = :collegeId
              and m.classroom.id = :classroomId
              and m.memberType = :memberType
            order by m.userId
            """)
    List<String> findMemberUserIdsByType(@Param("collegeId") UUID collegeId,
                                         @Param("classroomId") UUID classroomId,
                                         @Param("memberType") ClassroomMemberType memberType);

    @Query("""
            select m.classroom.id from ClassroomMember m
            where m.collegeId = :collegeId and m.userId = :userId
            """)
    List<UUID> findClassroomIdsOfUser(@Param("collegeId") UUID collegeId,
                                      @Param("userId") String userId);

    @Query("""
            select new com.CoreService.CoreService.classroom.dto.ClassroomMemberCount(
                m.classroom.id, count(m))
            from ClassroomMember m
            where m.collegeId = :collegeId and m.classroom.id in :classroomIds
            group by m.classroom.id
            """)
    List<ClassroomMemberCount> countMembersByClassroom(@Param("collegeId") UUID collegeId,
                                                       @Param("classroomIds") Collection<UUID> classroomIds);

    @Modifying
    @Query("delete from ClassroomMember m where m.collegeId = :collegeId and m.classroom.id = :classroomId")
    void deleteAllOfClassroom(@Param("collegeId") UUID collegeId, @Param("classroomId") UUID classroomId);
}
