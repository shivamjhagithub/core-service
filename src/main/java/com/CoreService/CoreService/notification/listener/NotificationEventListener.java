package com.CoreService.CoreService.notification.listener;

import com.CoreService.CoreService.announcement.event.AnnouncementPublishedEvent;
import com.CoreService.CoreService.assignment.event.AssignmentGradedEvent;
import com.CoreService.CoreService.assignment.event.AssignmentPublishedEvent;
import com.CoreService.CoreService.assignment.event.AssignmentSubmittedEvent;
import com.CoreService.CoreService.attendance.event.AttendanceMarkedEvent;
import com.CoreService.CoreService.chat.event.ChatMessageSentEvent;
import com.CoreService.CoreService.classroom.event.ClassroomCreatedEvent;
import com.CoreService.CoreService.classroom.event.ClassroomMemberAddedEvent;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.context.CollegeContext;
import com.CoreService.CoreService.common.context.UserContext;
import com.CoreService.CoreService.material.event.MaterialUploadedEvent;
import com.CoreService.CoreService.meeting.event.MeetingScheduledEvent;
import com.CoreService.CoreService.meeting.event.MeetingStartedEvent;
import com.CoreService.CoreService.notification.dto.NotificationRequest;
import com.CoreService.CoreService.notification.enums.NotificationType;
import com.CoreService.CoreService.notification.kafka.NotificationKafkaPublisher;
import com.CoreService.CoreService.notification.service.NotificationService;
import com.CoreService.CoreService.syllabus.event.SyllabusTopicCompletedEvent;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Turns domain events into notifications.
 * <p>
 * Every handler is a {@code @TransactionalEventListener}, so a notification is
 * only produced once the business transaction that raised the event has
 * actually committed - nobody is told about an assignment that was rolled back.
 * {@code @Async} then moves the work onto the domain event executor, which also
 * means the fan-out gets its own transaction instead of trying to join one that
 * has already completed.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private static final String UNTITLED = "Untitled";

    private final NotificationService notificationService;
    private final ClassroomAccessService classroomAccessService;
    private final UserRepo userRepo;
    private final ObjectProvider<NotificationKafkaPublisher> kafkaPublisher;

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onClassroomCreated(ClassroomCreatedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(without(members(event.collegeId(), event.classroomId()), event.createdBy()))
                .type(NotificationType.CLASSROOM_CREATED)
                .title("New classroom")
                .body("The classroom \"" + label(event.classroomName()) + "\" is now available.")
                .referenceId(event.classroomId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onClassroomMemberAdded(ClassroomMemberAddedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(event.memberUserIds())
                .type(NotificationType.CLASSROOM_MEMBER_ADDED)
                .title("Added to a classroom")
                .body("You have been added to \"" + label(event.classroomName()) + "\" as a "
                        + role(event.memberType()) + ".")
                .referenceId(event.classroomId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAttendanceMarked(AttendanceMarkedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(event.studentUserIds())
                .type(NotificationType.ATTENDANCE_MARKED)
                .title("Attendance recorded")
                .body("Your attendance has been recorded for the latest session.")
                .referenceId(event.attendanceSessionId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAssignmentPublished(AssignmentPublishedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(students(event.collegeId(), event.classroomId()))
                .type(NotificationType.ASSIGNMENT_CREATED)
                .title("New assignment")
                .body("\"" + label(event.title()) + "\" has been published." + deadline(event.dueAt()))
                .referenceId(event.assignmentId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAssignmentSubmitted(AssignmentSubmittedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(teachers(event.collegeId(), event.classroomId()))
                .type(NotificationType.ASSIGNMENT_SUBMITTED)
                .title("New submission")
                .body(displayName(event.collegeId(), event.studentUserId()) + " submitted an assignment"
                        + (event.late() ? " after the deadline." : "."))
                .referenceId(event.assignmentId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAssignmentGraded(AssignmentGradedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(only(event.studentUserId()))
                .type(NotificationType.ASSIGNMENT_GRADED)
                .title("Assignment graded")
                .body("Your submission for \"" + label(event.title()) + "\" has been graded"
                        + (event.marks() == null ? "." : ": " + event.marks() + " marks."))
                .referenceId(event.assignmentId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onMaterialUploaded(MaterialUploadedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(students(event.collegeId(), event.classroomId()))
                .type(NotificationType.MATERIAL_UPLOADED)
                .title("New study material")
                .body("\"" + label(event.title()) + "\" has been added to your classroom.")
                .referenceId(event.materialId())
                .build());
    }

    /**
     * The syllabus event carries no audience and the published classroom API
     * cannot resolve one from a syllabus id, so this is delivered as a live
     * college-wide signal instead of a stored per-user notification.
     */
    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onSyllabusTopicCompleted(SyllabusTopicCompletedEvent event) {
        try {
            notificationService.broadcastToCollege(event.collegeId(),
                    NotificationType.SYLLABUS_UPDATED,
                    "Syllabus updated",
                    "The topic \"" + label(event.topicTitle()) + "\" has been marked as completed.",
                    event.syllabusId());
        } catch (RuntimeException ex) {
            log.warn("Notification fan-out for {} failed: {}", event.eventKey(), ex.toString());
        }
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onChatMessageSent(ChatMessageSentEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                // The sender already sees their own message in the room.
                .recipientUserIds(without(event.recipientUserIds(), event.senderUserId()))
                .type(NotificationType.CHAT_MESSAGE)
                .title("New message from " + displayName(event.collegeId(), event.senderUserId()))
                .body(label(event.preview()))
                .referenceId(event.chatRoomId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onMeetingScheduled(MeetingScheduledEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(members(event.collegeId(), event.classroomId()))
                .type(NotificationType.MEETING_SCHEDULED)
                .title("Meeting scheduled")
                .body("\"" + label(event.title()) + "\" is scheduled for "
                        + at(event.scheduledStartTime()) + ".")
                .referenceId(event.meetingId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onMeetingStarted(MeetingStartedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(members(event.collegeId(), event.classroomId()))
                .type(NotificationType.MEETING_STARTED)
                .title("Meeting started")
                .body("\"" + label(event.title()) + "\" has started. Join now.")
                .referenceId(event.meetingId())
                .build());
    }

    @Async("domainEventExecutor")
    @TransactionalEventListener
    public void onAnnouncementPublished(AnnouncementPublishedEvent event) {
        handle(event.eventKey(), () -> NotificationRequest.builder()
                .collegeId(event.collegeId())
                .recipientUserIds(event.recipientUserIds())
                .type(NotificationType.ANNOUNCEMENT)
                .title("Announcement")
                .body(label(event.title()))
                .referenceId(event.announcementId())
                .build());
    }

    /**
     * Where the fan-out actually runs. With the broker enabled the request is
     * handed to Kafka and the consumer dispatches it, otherwise it is dispatched
     * straight away - either way this stays inside the one application.
     */
    private void deliver(String eventKey, NotificationRequest request) {
        NotificationKafkaPublisher publisher = kafkaPublisher.getIfAvailable();
        if (publisher != null) {
            publisher.publish(eventKey, request);
            return;
        }
        notificationService.dispatch(request);
    }

    private void handle(String eventKey, Supplier<NotificationRequest> request) {
        try {
            deliver(eventKey, request.get());
        } catch (RuntimeException ex) {
            log.warn("Notification fan-out for {} failed: {}", eventKey, ex.toString());
        }
    }

    private List<String> members(UUID collegeId, UUID classroomId) {
        return inTenant(collegeId, () -> classroomAccessService.memberUserIds(classroomId));
    }

    private List<String> students(UUID collegeId, UUID classroomId) {
        return inTenant(collegeId, () -> classroomAccessService.studentUserIds(classroomId));
    }

    private List<String> teachers(UUID collegeId, UUID classroomId) {
        return inTenant(collegeId, () -> classroomAccessService.teacherUserIds(classroomId));
    }

    /**
     * Handlers run on a pooled thread after commit, where the request scoped
     * tenant is long gone. The college is therefore taken from the event payload
     * and published into the context for the duration of the lookup, because the
     * classroom and user APIs resolve their tenant from it.
     */
    private <T> T inTenant(UUID collegeId, Supplier<T> work) {
        CollegeContext.setCollegeId(collegeId);
        UserContext.setCollegeId(collegeId);
        try {
            return work.get();
        } finally {
            CollegeContext.clear();
            UserContext.clear();
        }
    }

    private String displayName(UUID collegeId, String userId) {
        if (userId == null || userId.isBlank()) {
            return "Someone";
        }
        return userRepo.findByUserIdAndCollegeId(userId, collegeId)
                .map(UserEntity::getUserName)
                .filter(name -> !name.isBlank())
                .orElse(userId);
    }

    private List<String> only(String userId) {
        return userId == null || userId.isBlank() ? List.of() : List.of(userId);
    }

    private List<String> without(List<String> userIds, String excluded) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        if (excluded == null || excluded.isBlank()) {
            return userIds;
        }
        return userIds.stream().filter(userId -> !excluded.equals(userId)).toList();
    }

    private String deadline(Instant dueAt) {
        return dueAt == null ? "" : " It is due on " + WHEN.format(dueAt) + ".";
    }

    private String at(Instant instant) {
        return instant == null ? "a later time" : WHEN.format(instant);
    }

    private String role(Enum<?> memberType) {
        return memberType == null ? "member" : memberType.name().toLowerCase(Locale.ROOT);
    }

    private String label(String value) {
        return value == null || value.isBlank() ? UNTITLED : value;
    }
}
