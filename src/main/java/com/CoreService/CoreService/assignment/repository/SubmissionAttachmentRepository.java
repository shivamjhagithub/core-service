package com.CoreService.CoreService.assignment.repository;

import com.CoreService.CoreService.assignment.dto.AttachmentRow;
import com.CoreService.CoreService.assignment.entity.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, UUID> {

    @Query("""
            select new com.CoreService.CoreService.assignment.dto.AttachmentRow(sa.submission.id, sa.fileId)
            from SubmissionAttachment sa
            where sa.collegeId = :collegeId and sa.submission.id in :submissionIds
            order by sa.createdAt asc
            """)
    List<AttachmentRow> findRowsBySubmissionIds(@Param("collegeId") UUID collegeId,
                                                @Param("submissionIds") Collection<UUID> submissionIds);

    @Modifying
    @Query("delete from SubmissionAttachment sa where sa.collegeId = :collegeId and sa.submission.id = :submissionId")
    void deleteBySubmission(@Param("collegeId") UUID collegeId, @Param("submissionId") UUID submissionId);
}
