package com.CoreService.CoreService.meeting.service;

import com.CoreService.CoreService.meeting.enums.ParticipantRole;

import java.util.UUID;

/**
 * Backend management of conference rooms. The ERP schedules, authorizes and
 * records meetings; it never carries audio or video, so no implementation of
 * this interface is expected to touch media. Media flows peer to peer, or
 * through whichever external service a deployment configures.
 * <p>
 * Selected by the {@code app.meeting.provider} property. Adding a hosted
 * provider means adding one bean, conditional on that property, with no change
 * to the service or the entities.
 */
public interface VideoConferenceProvider {

    /** Value of {@code app.meeting.provider} this implementation answers to. */
    String name();

    /**
     * Reserves the provider-side room for a meeting.
     *
     * @return the provider's room identifier, stored on the meeting
     */
    String createRoom(UUID meetingId, String title);

    /**
     * Credential or handle a client presents to the provider when joining.
     *
     * @return an opaque token; its meaning belongs entirely to the provider
     */
    String joinToken(UUID meetingId, String userId, ParticipantRole role);

    /** Releases provider-side resources once a meeting has ended. */
    void closeRoom(String providerRoomId);
}
