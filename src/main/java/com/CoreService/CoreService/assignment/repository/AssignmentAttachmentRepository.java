package com.CoreService.CoreService.assignment.repository;

import com.CoreService.CoreService.assignment.dto.AttachmentRow;
import com.CoreService.CoreService.assignment.entity.AssignmentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, UUID> {

    @Query("""
            select new com.CoreService.CoreService.assignment.dto.AttachmentRow(a.assignment.id, a.fileId)
            from AssignmentAttachment a
            where a.collegeId = :collegeId and a.assignment.id in :assignmentIds
            order by a.createdAt asc
            """)
    List<AttachmentRow> findRowsByAssignmentIds(@Param("collegeId") UUID collegeId,
                                                @Param("assignmentIds") Collection<UUID> assignmentIds);

    @Modifying
    @Query("delete from AssignmentAttachment a where a.collegeId = :collegeId and a.assignment.id = :assignmentId")
    void deleteByAssignment(@Param("collegeId") UUID collegeId, @Param("assignmentId") UUID assignmentId);
}
