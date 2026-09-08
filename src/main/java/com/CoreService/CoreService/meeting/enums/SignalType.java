package com.CoreService.CoreService.meeting.enums;

/**
 * WebRTC signaling frames the server relays between peers. OFFER, ANSWER and
 * ICE_CANDIDATE are addressed to a single peer; JOIN and LEAVE are announced to
 * the whole meeting.
 */
public enum SignalType {
    JOIN,
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    LEAVE
}
