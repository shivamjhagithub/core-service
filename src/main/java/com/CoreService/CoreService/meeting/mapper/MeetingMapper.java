package com.CoreService.CoreService.meeting.mapper;

import com.CoreService.CoreService.meeting.dto.MeetingDetailResponse;
import com.CoreService.CoreService.meeting.dto.MeetingParticipantResponse;
import com.CoreService.CoreService.meeting.dto.MeetingResponse;
import com.CoreService.CoreService.meeting.entity.Meeting;
import com.CoreService.CoreService.meeting.entity.MeetingParticipant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MeetingMapper {

    public MeetingResponse toResponse(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getClassroomId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getHostUserId(),
                meeting.getScheduledStartTime(),
                meeting.getScheduledEndTime(),
                meeting.getActualStartTime(),
                meeting.getActualEndTime(),
                meeting.getStatus(),
                meeting.getProviderName());
    }

    public MeetingDetailResponse toDetailResponse(Meeting meeting, List<MeetingParticipant> roster) {
        return new MeetingDetailResponse(
                toResponse(meeting),
                roster.stream().map(this::toParticipantResponse).toList());
    }

    public MeetingParticipantResponse toParticipantResponse(MeetingParticipant participant) {
        return new MeetingParticipantResponse(
                participant.getUserId(),
                participant.getRole(),
                participant.getJoinedAt(),
                participant.getLeftAt());
    }
}
