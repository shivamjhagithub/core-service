package com.CoreService.CoreService.meeting.service;

import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.ForbiddenException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.common.websocket.WebSocketDestinations;
import com.CoreService.CoreService.meeting.dto.MeetingDetailResponse;
import com.CoreService.CoreService.meeting.dto.MeetingJoinResponse;
import com.CoreService.CoreService.meeting.dto.MeetingResponse;
import com.CoreService.CoreService.meeting.dto.MeetingStatusEvent;
import com.CoreService.CoreService.meeting.dto.ScheduleMeetingRequest;
import com.CoreService.CoreService.meeting.dto.UpdateMeetingRequest;
import com.CoreService.CoreService.meeting.entity.Meeting;
import com.CoreService.CoreService.meeting.entity.MeetingParticipant;
import com.CoreService.CoreService.meeting.enums.MeetingStatus;
import com.CoreService.CoreService.meeting.enums.ParticipantRole;
import com.CoreService.CoreService.meeting.event.MeetingScheduledEvent;
import com.CoreService.CoreService.meeting.event.MeetingStartedEvent;
import com.CoreService.CoreService.meeting.mapper.MeetingMapper;
import com.CoreService.CoreService.meeting.repository.MeetingParticipantRepository;
import com.CoreService.CoreService.meeting.repository.MeetingRepository;
import com.CoreService.CoreService.meeting.websocket.MeetingDestinations;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Meeting lifecycle and access control. The application schedules and authorizes
 * meetings and relays signaling; media never passes through it, which is why the
 * only provider-specific state kept here is a room id and a join token.
 */
@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final String MEETING = "Meeting";
    private static final String PARTICIPANT = "Meeting participant";

    /** How early a scheduled meeting can be entered. */
    private static final Duration JOIN_LEAD_TIME = Duration.ofMinutes(15);
    /** How long after its planned end a meeting still accepts joins. */
    private static final Duration JOIN_GRACE = Duration.ofMinutes(30);
    /** Join window for a meeting scheduled without an end time. */
    private static final Duration OPEN_ENDED_WINDOW = Duration.ofHours(4);
    /** Keeps a meeting that has already begun in the caller's upcoming list. */
    private static final Duration UPCOMING_LOOKBACK = Duration.ofHours(4);

    private static final Set<MeetingStatus> ACTIVE_STATUSES =
            EnumSet.of(MeetingStatus.SCHEDULED, MeetingStatus.LIVE);

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingMapper meetingMapper;
    private final ClassroomAccessService classroomAccessService;
    private final ModuleAccessGuard moduleAccessGuard;
    private final TenantGuard tenantGuard;
    private final VideoConferenceProvider videoConferenceProvider;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MeetingResponse schedule(ScheduleMeetingRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String hostUserId = tenantGuard.requireUserId();

        if (request.classroomId() != null) {
            classroomAccessService.assertPermission(request.classroomId(),
                    ClassroomPermissionType.CREATE_MEETING);
        }
        assertSchedulingWindow(request.scheduledStartTime(), request.scheduledEndTime());

        Meeting draft = Meeting.builder()
                .classroomId(request.classroomId())
                .title(request.title())
                .description(request.description())
                .hostUserId(hostUserId)
                .scheduledStartTime(request.scheduledStartTime())
                .scheduledEndTime(request.scheduledEndTime())
                .status(MeetingStatus.SCHEDULED)
                .providerName(videoConferenceProvider.name())
                .build();
        draft.setCollegeId(collegeId);

        // Providers key their room on the meeting id, so the row is written
        // first; the room id is flushed with the rest of the transaction.
        Meeting meeting = meetingRepository.save(draft);
        meeting.setProviderRoomId(videoConferenceProvider.createRoom(meeting.getId(), meeting.getTitle()));

        meetingParticipantRepository.save(
                newParticipant(meeting, collegeId, hostUserId, ParticipantRole.HOST));

        eventPublisher.publishEvent(new MeetingScheduledEvent(
                collegeId,
                meeting.getId(),
                meeting.getClassroomId(),
                meeting.getTitle(),
                meeting.getScheduledStartTime(),
                Instant.now()));

        return meetingMapper.toResponse(meeting);
    }

    @Transactional
    public MeetingResponse update(UUID meetingId, UpdateMeetingRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        Meeting meeting = requireMeeting(meetingId, collegeId);
        assertHostOrClassroomTeacher(meeting, callerId);

        if (meeting.getStatus() != MeetingStatus.SCHEDULED) {
            throw new BusinessException("Only a scheduled meeting can be rescheduled, this one is "
                    + meeting.getStatus());
        }
        assertSchedulingWindow(request.scheduledStartTime(), request.scheduledEndTime());

        meeting.setTitle(request.title());
        meeting.setDescription(request.description());
        meeting.setScheduledStartTime(request.scheduledStartTime());
        meeting.setScheduledEndTime(request.scheduledEndTime());

        return meetingMapper.toResponse(meetingRepository.save(meeting));
    }

    @Transactional
    public MeetingResponse cancel(UUID meetingId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        Meeting meeting = requireMeeting(meetingId, collegeId);
        assertHostOrClassroomTeacher(meeting, callerId);

        if (meeting.getStatus() == MeetingStatus.ENDED || meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new BusinessException("This meeting is already " + meeting.getStatus());
        }

        Instant now = Instant.now();
        boolean wasLive = meeting.getStatus() == MeetingStatus.LIVE;
        meeting.setStatus(MeetingStatus.CANCELLED);
        if (wasLive) {
            meeting.setActualEndTime(now);
        }
        meetingRepository.save(meeting);
        closeOut(meeting, collegeId, now);

        return meetingMapper.toResponse(meeting);
    }

    @Transactional
    public MeetingResponse start(UUID meetingId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        Meeting meeting = requireMeeting(meetingId, collegeId);
        assertHostOrClassroomTeacher(meeting, callerId);

        if (meeting.getStatus() != MeetingStatus.SCHEDULED) {
            throw new BusinessException("Only a scheduled meeting can be started, this one is "
                    + meeting.getStatus());
        }

        Instant now = Instant.now();
        meeting.setStatus(MeetingStatus.LIVE);
        meeting.setActualStartTime(now);
        meetingRepository.save(meeting);

        eventPublisher.publishEvent(new MeetingStartedEvent(
                collegeId, meeting.getId(), meeting.getClassroomId(), meeting.getTitle(), now));
        announceStatus(meeting, now);

        return meetingMapper.toResponse(meeting);
    }

    @Transactional
    public MeetingResponse end(UUID meetingId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        Meeting meeting = requireMeeting(meetingId, collegeId);
        assertHostOrClassroomTeacher(meeting, callerId);

        if (meeting.getStatus() != MeetingStatus.LIVE) {
            throw new BusinessException("Only a live meeting can be ended, this one is "
                    + meeting.getStatus());
        }

        Instant now = Instant.now();
        meeting.setStatus(MeetingStatus.ENDED);
        meeting.setActualEndTime(now);
        meetingRepository.save(meeting);
        closeOut(meeting, collegeId, now);

        return meetingMapper.toResponse(meeting);
    }

    /**
     * Admits the caller and returns what the client needs to connect. Joining
     * does not start a meeting: only its host or a classroom teacher does that.
     */
    @Transactional
    public MeetingJoinResponse join(UUID meetingId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        Meeting meeting = requireMeeting(meetingId, collegeId);
        assertWithinJoinWindow(meeting);

        Optional<MeetingParticipant> existing =
                meetingParticipantRepository.findParticipant(collegeId, meetingId, callerId);
        assertMayJoin(meeting, callerId, existing.isPresent());

        Instant now = Instant.now();
        MeetingParticipant participant = existing.orElseGet(() -> newParticipant(meeting, collegeId, callerId,
                meeting.getHostUserId().equals(callerId) ? ParticipantRole.HOST : ParticipantRole.ATTENDEE));
        participant.setJoinedAt(now);
        participant.setLeftAt(null);
        meetingParticipantRepository.save(participant);

        return new MeetingJoinResponse(
                meeting.getId(),
                participant.getRole(),
                meeting.getProviderName(),
                meeting.getProviderRoomId(),
                videoConferenceProvider.joinToken(meeting.getId(), callerId, participant.getRole()),
                MeetingDestinations.SIGNAL_DESTINATION,
                WebSocketDestinations.meetingTopic(meeting.getId()),
                MeetingDestinations.SIGNAL_QUEUE);
    }

    @Transactional
    public void leave(UUID meetingId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        MeetingParticipant participant = meetingParticipantRepository
                .findParticipant(collegeId, meetingId, callerId)
                .orElseThrow(() -> new ResourceNotFoundException(PARTICIPANT, callerId));

        participant.setLeftAt(Instant.now());
        meetingParticipantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public MeetingDetailResponse detail(UUID meetingId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        Meeting meeting = requireMeeting(meetingId, collegeId);
        List<MeetingParticipant> roster = meetingParticipantRepository.findRoster(collegeId, meetingId);
        assertMayView(meeting, callerId, roster);

        return meetingMapper.toDetailResponse(meeting, roster);
    }

    /**
     * Meetings the caller can attend next: the ones they host plus the ones
     * scheduled for their classrooms.
     */
    @Transactional(readOnly = true)
    public List<MeetingResponse> upcoming() {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();
        Instant from = Instant.now().minus(UPCOMING_LOOKBACK);

        Map<UUID, Meeting> merged = new LinkedHashMap<>();
        meetingRepository.findUpcomingHostedBy(collegeId, callerId, ACTIVE_STATUSES, from)
                .forEach(meeting -> merged.put(meeting.getId(), meeting));

        List<UUID> classroomIds = classroomAccessService.classroomIdsOfUser(callerId);
        if (!classroomIds.isEmpty()) {
            meetingRepository.findUpcomingForClassrooms(collegeId, classroomIds, ACTIVE_STATUSES, from)
                    .forEach(meeting -> merged.putIfAbsent(meeting.getId(), meeting));
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(Meeting::getScheduledStartTime))
                .map(meetingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MeetingResponse> byClassroom(UUID classroomId, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.VIDEO_CONFERENCE);

        UUID collegeId = tenantGuard.requireCollegeId();
        classroomAccessService.requireClassroom(classroomId);
        classroomAccessService.assertMember(classroomId);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "scheduledStartTime", "id"));
        Page<Meeting> meetings = meetingRepository
                .findByCollegeIdAndClassroomId(collegeId, classroomId, pageable);

        return PageResponse.from(meetings, meetingMapper::toResponse);
    }

    /**
     * Whether a session may relay signaling for a meeting. Context-free so it
     * can run on a WebSocket thread.
     * <p>
     * Presence alone is sufficient: ending or cancelling a meeting marks every
     * participant as having left, so a finished meeting has no active
     * participants and stops relaying without a second lookup per frame.
     */
    @Transactional(readOnly = true)
    public boolean isActiveParticipant(UUID meetingId, UUID collegeId, String userId) {
        return meetingParticipantRepository.isActiveParticipant(collegeId, meetingId, userId);
    }

    private Meeting requireMeeting(UUID meetingId, UUID collegeId) {
        return meetingRepository.findByIdAndCollegeId(meetingId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(MEETING, meetingId));
    }

    private MeetingParticipant newParticipant(Meeting meeting,
                                              UUID collegeId,
                                              String userId,
                                              ParticipantRole role) {
        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .userId(userId)
                .role(role)
                .build();
        participant.setCollegeId(collegeId);
        return participant;
    }

    private void assertSchedulingWindow(Instant start, Instant end) {
        if (start == null || !start.isAfter(Instant.now())) {
            throw new BusinessException("A meeting must be scheduled in the future");
        }
        if (end != null && !end.isAfter(start)) {
            throw new BusinessException("A meeting must end after it starts");
        }
    }

    private void assertHostOrClassroomTeacher(Meeting meeting, String callerId) {
        if (meeting.getHostUserId().equals(callerId)) {
            return;
        }
        if (meeting.getClassroomId() != null
                && classroomAccessService.isTeacher(meeting.getClassroomId(), callerId)) {
            return;
        }
        throw new ForbiddenException("Only the host or a teacher of the classroom can manage this meeting");
    }

    private void assertWithinJoinWindow(Meeting meeting) {
        Instant now = Instant.now();
        switch (meeting.getStatus()) {
            case LIVE -> {
                // A live meeting is joinable for as long as it runs.
            }
            case SCHEDULED -> {
                if (now.isBefore(meeting.getScheduledStartTime().minus(JOIN_LEAD_TIME))) {
                    throw new BusinessException("This meeting is not open for joining yet");
                }
                if (now.isAfter(joinWindowEnd(meeting))) {
                    throw new BusinessException("The join window for this meeting has closed");
                }
            }
            case ENDED, CANCELLED ->
                    throw new BusinessException("This meeting is " + meeting.getStatus());
        }
    }

    private Instant joinWindowEnd(Meeting meeting) {
        return meeting.getScheduledEndTime() != null
                ? meeting.getScheduledEndTime().plus(JOIN_GRACE)
                : meeting.getScheduledStartTime().plus(OPEN_ENDED_WINDOW);
    }

    private void assertMayJoin(Meeting meeting, String callerId, boolean onRoster) {
        if (meeting.getHostUserId().equals(callerId)) {
            return;
        }
        if (meeting.getClassroomId() != null) {
            classroomAccessService.assertMember(meeting.getClassroomId());
            return;
        }
        if (!onRoster) {
            throw new ForbiddenException("This meeting is limited to its host and invited participants");
        }
    }

    private void assertMayView(Meeting meeting, String callerId, List<MeetingParticipant> roster) {
        if (meeting.getHostUserId().equals(callerId)) {
            return;
        }
        if (meeting.getClassroomId() != null) {
            classroomAccessService.assertMember(meeting.getClassroomId());
            return;
        }
        boolean onRoster = roster.stream()
                .anyMatch(participant -> participant.getUserId().equals(callerId));
        if (!onRoster) {
            throw new ForbiddenException("You are not a participant of this meeting");
        }
    }

    /** Releases the provider room and closes out anyone still marked present. */
    private void closeOut(Meeting meeting, UUID collegeId, Instant at) {
        meetingParticipantRepository.markAllLeft(collegeId, meeting.getId(), at);
        if (meeting.getProviderRoomId() != null) {
            videoConferenceProvider.closeRoom(meeting.getProviderRoomId());
        }
        announceStatus(meeting, at);
    }

    private void announceStatus(Meeting meeting, Instant at) {
        messagingTemplate.convertAndSend(WebSocketDestinations.meetingTopic(meeting.getId()),
                new MeetingStatusEvent(meeting.getId(), meeting.getStatus(), at));
    }
}
