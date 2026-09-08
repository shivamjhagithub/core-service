package com.CoreService.CoreService.audit.listener;

import com.CoreService.CoreService.announcement.event.AnnouncementPublishedEvent;
import com.CoreService.CoreService.assignment.event.AssignmentGradedEvent;
import com.CoreService.CoreService.assignment.event.AssignmentPublishedEvent;
import com.CoreService.CoreService.assignment.event.AssignmentSubmittedEvent;
import com.CoreService.CoreService.attendance.event.AttendanceMarkedEvent;
import com.CoreService.CoreService.audit.enums.AuditAction;
import com.CoreService.CoreService.audit.service.AuditService;
import com.CoreService.CoreService.chat.event.ChatMessageSentEvent;
import com.CoreService.CoreService.classroom.event.ClassroomCreatedEvent;
import com.CoreService.CoreService.classroom.event.ClassroomMemberAddedEvent;
import com.CoreService.CoreService.material.event.MaterialUploadedEvent;
import com.CoreService.CoreService.meeting.event.MeetingScheduledEvent;
import com.CoreService.CoreService.meeting.event.MeetingStartedEvent;
import com.CoreService.CoreService.syllabus.event.SyllabusTopicCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Records what other modules did, from the events they publish.
 * <p>
 * Listening after commit is what makes the trail truthful: an attempt that was
 * rolled back never raises its event, so it never turns into an audit line. The
 * handlers run on the domain event executor, where the request context no
 * longer exists, so the tenant is read from the event itself.
 */
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private static final String RESOURCE_CLASSROOM = "Classroom";
    private static final String RESOURCE_ATTENDANCE_SESSION = "AttendanceSession";
    private static final String RESOURCE_ASSIGNMENT = "Assignment";
    private static final String RESOURCE_SUBMISSION = "AssignmentSubmission";
    private static final String RESOURCE_MATERIAL = "StudyMaterial";
    private static final String RESOURCE_SYLLABUS_TOPIC = "SyllabusTopic";
    private static final String RESOURCE_CHAT_MESSAGE = "ChatMessage";
    private static final String RESOURCE_MEETING = "Meeting";
    private static final String RESOURCE_ANNOUNCEMENT = "Announcement";

    private final AuditService auditService;

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onClassroomCreated(ClassroomCreatedEvent event) {
        record(event.collegeId(), event.createdBy(), AuditAction.CLASSROOM_CREATED,
                RESOURCE_CLASSROOM, event.classroomId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onClassroomMemberAdded(ClassroomMemberAddedEvent event) {
        record(event.collegeId(), null, AuditAction.CLASSROOM_MEMBER_ADDED,
                RESOURCE_CLASSROOM, event.classroomId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAttendanceMarked(AttendanceMarkedEvent event) {
        record(event.collegeId(), null, AuditAction.ATTENDANCE_MARKED,
                RESOURCE_ATTENDANCE_SESSION, event.attendanceSessionId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAssignmentPublished(AssignmentPublishedEvent event) {
        record(event.collegeId(), null, AuditAction.ASSIGNMENT_CREATED,
                RESOURCE_ASSIGNMENT, event.assignmentId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAssignmentSubmitted(AssignmentSubmittedEvent event) {
        record(event.collegeId(), event.studentUserId(), AuditAction.ASSIGNMENT_SUBMITTED,
                RESOURCE_SUBMISSION, event.submissionId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAssignmentGraded(AssignmentGradedEvent event) {
        // The event names the graded student, not the grader, so no actor is claimed.
        record(event.collegeId(), null, AuditAction.ASSIGNMENT_GRADED,
                RESOURCE_SUBMISSION, event.submissionId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onMaterialUploaded(MaterialUploadedEvent event) {
        record(event.collegeId(), null, AuditAction.MATERIAL_UPLOADED,
                RESOURCE_MATERIAL, event.materialId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onSyllabusTopicCompleted(SyllabusTopicCompletedEvent event) {
        record(event.collegeId(), null, AuditAction.SYLLABUS_TOPIC_COMPLETED,
                RESOURCE_SYLLABUS_TOPIC, event.topicId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onChatMessageSent(ChatMessageSentEvent event) {
        // Only the identifiers are recorded; the message text stays out of the trail.
        record(event.collegeId(), event.senderUserId(), AuditAction.CHAT_MESSAGE_SENT,
                RESOURCE_CHAT_MESSAGE, event.messageId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onMeetingScheduled(MeetingScheduledEvent event) {
        record(event.collegeId(), null, AuditAction.MEETING_CREATED, RESOURCE_MEETING, event.meetingId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onMeetingStarted(MeetingStartedEvent event) {
        record(event.collegeId(), null, AuditAction.MEETING_STARTED, RESOURCE_MEETING, event.meetingId());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAnnouncementPublished(AnnouncementPublishedEvent event) {
        record(event.collegeId(), null, AuditAction.ANNOUNCEMENT_PUBLISHED,
                RESOURCE_ANNOUNCEMENT, event.announcementId());
    }

    private void record(UUID collegeId, String userId, String action, String resourceType, UUID resourceId) {
        try {
            auditService.recordForCollege(collegeId, userId, action, resourceType,
                    resourceId == null ? null : resourceId.toString());
        } catch (RuntimeException ex) {
            log.error("Could not record audit entry {} for {} {}", action, resourceType, resourceId, ex);
        }
    }
}
