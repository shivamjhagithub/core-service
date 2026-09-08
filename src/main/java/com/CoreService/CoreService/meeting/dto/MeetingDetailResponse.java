package com.CoreService.CoreService.meeting.dto;

import java.util.List;

public record MeetingDetailResponse(MeetingResponse meeting,
                                    List<MeetingParticipantResponse> participants) {
}
