package com.CoreService.CoreService.attendance.repository;

import com.CoreService.CoreService.attendance.dto.AttendanceStatusTally;
import com.CoreService.CoreService.attendance.dto.SessionStatusTally;
import com.CoreService.CoreService.attendance.dto.SubjectStatusTally;
import com.CoreService.CoreService.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findByIdAndCollegeId(UUID id, UUID collegeId);

    List<AttendanceRecord> findAllByCollegeIdAndSession_IdOrderByStudentUserIdAsc(UUID collegeId, UUID sessionId);

    List<AttendanceRecord> findAllByCollegeIdAndSession_IdAndStudentUserIdIn(UUID collegeId,
                                                                             UUID sessionId,
                                                                             Collection<String> studentUserIds);

    @Query("""
            select new com.CoreService.CoreService.attendance.dto.AttendanceStatusTally(
                r.studentUserId, r.status, count(r))
            from AttendanceRecord r
            where r.collegeId = :collegeId
              and r.session.classroomId = :classroomId
            group by r.studentUserId, r.status
            """)
    List<AttendanceStatusTally> tallyByClassroom(@Param("collegeId") UUID collegeId,
                                                 @Param("classroomId") UUID classroomId);

    @Query("""
            select new com.CoreService.CoreService.attendance.dto.AttendanceStatusTally(
                r.studentUserId, r.status, count(r))
            from AttendanceRecord r
            where r.collegeId = :collegeId
              and r.session.classroomId = :classroomId
              and r.studentUserId = :studentUserId
            group by r.studentUserId, r.status
            """)
    List<AttendanceStatusTally> tallyByClassroomAndStudent(@Param("collegeId") UUID collegeId,
                                                           @Param("classroomId") UUID classroomId,
                                                           @Param("studentUserId") String studentUserId);

    @Query("""
            select new com.CoreService.CoreService.attendance.dto.SessionStatusTally(
                r.session.id, r.status, count(r))
            from AttendanceRecord r
            where r.collegeId = :collegeId
              and r.session.id in :sessionIds
            group by r.session.id, r.status
            """)
    List<SessionStatusTally> tallyBySessions(@Param("collegeId") UUID collegeId,
                                             @Param("sessionIds") Collection<UUID> sessionIds);

    @Query("""
            select new com.CoreService.CoreService.attendance.dto.SubjectStatusTally(
                s.subjectId, r.status, count(r))
            from AttendanceRecord r
            join r.session s
            where r.collegeId = :collegeId
              and r.studentUserId = :studentUserId
            group by s.subjectId, r.status
            """)
    List<SubjectStatusTally> tallySubjectsOfStudent(@Param("collegeId") UUID collegeId,
                                                    @Param("studentUserId") String studentUserId);

    @Query("""
            select new com.CoreService.CoreService.attendance.dto.SubjectStatusTally(
                s.subjectId, r.status, count(r))
            from AttendanceRecord r
            join r.session s
            where r.collegeId = :collegeId
              and r.studentUserId = :studentUserId
              and s.classroomId = :classroomId
            group by s.subjectId, r.status
            """)
    List<SubjectStatusTally> tallySubjectsOfStudentInClassroom(@Param("collegeId") UUID collegeId,
                                                               @Param("studentUserId") String studentUserId,
                                                               @Param("classroomId") UUID classroomId);

    @Query("""
            select r from AttendanceRecord r
            join fetch r.session s
            where r.collegeId = :collegeId
              and r.studentUserId = :studentUserId
            order by s.sessionDate desc
            """)
    List<AttendanceRecord> findStudentHistory(@Param("collegeId") UUID collegeId,
                                              @Param("studentUserId") String studentUserId);

    @Query("""
            select r from AttendanceRecord r
            join fetch r.session s
            where r.collegeId = :collegeId
              and r.studentUserId = :studentUserId
              and s.classroomId = :classroomId
            order by s.sessionDate desc
            """)
    List<AttendanceRecord> findStudentHistoryInClassroom(@Param("collegeId") UUID collegeId,
                                                         @Param("studentUserId") String studentUserId,
                                                         @Param("classroomId") UUID classroomId);
}
