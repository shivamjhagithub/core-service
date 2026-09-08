package com.CoreService.CoreService.chat.service;

import com.CoreService.CoreService.chat.dto.ChatMessageResponse;
import com.CoreService.CoreService.chat.dto.ChatPresenceEvent;
import com.CoreService.CoreService.chat.dto.ChatRoomResponse;
import com.CoreService.CoreService.chat.dto.ChatUnreadCountResponse;
import com.CoreService.CoreService.chat.entity.ChatMessage;
import com.CoreService.CoreService.chat.entity.ChatParticipant;
import com.CoreService.CoreService.chat.entity.ChatRoom;
import com.CoreService.CoreService.chat.enums.ChatPermissionMode;
import com.CoreService.CoreService.chat.enums.ChatRoomType;
import com.CoreService.CoreService.chat.enums.MessageType;
import com.CoreService.CoreService.chat.event.ChatMessageSentEvent;
import com.CoreService.CoreService.chat.mapper.ChatMapper;
import com.CoreService.CoreService.chat.repository.ChatMessageRepository;
import com.CoreService.CoreService.chat.repository.ChatParticipantRepository;
import com.CoreService.CoreService.chat.repository.ChatRoomRepository;
import com.CoreService.CoreService.classroom.dto.ClassroomRef;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.ForbiddenException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.common.websocket.WebSocketDestinations;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Chat use cases. All tenant and membership decisions are taken here so the REST
 * controller and the STOMP controller cannot drift apart: both transports enter
 * through the same methods and both pass an identity that the transport layer
 * established, never one taken from a payload.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ROOM = "Chat room";
    private static final String MESSAGE = "Chat message";
    private static final int PREVIEW_LENGTH = 120;
    private static final int MAX_CONTENT_LENGTH = 4000;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMapper chatMapper;
    private final ClassroomAccessService classroomAccessService;
    private final ModuleAccessGuard moduleAccessGuard;
    private final TenantGuard tenantGuard;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Opens (or reuses) the one-to-one room between the caller and a peer of the
     * same college.
     */
    @Transactional
    public ChatRoomResponse openDirectRoom(String otherUserId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        if (callerId.equals(otherUserId)) {
            throw new BusinessException("A direct chat needs two different users");
        }
        // Reported as "not found" rather than "forbidden": a caller must not be
        // able to probe which user ids exist in other colleges.
        if (!userRepo.existsByCollegeIdAndUserId(collegeId, otherUserId)) {
            throw new ResourceNotFoundException("User", otherUserId);
        }
        if (!userRepo.existsByCollegeIdAndUserId(collegeId, callerId)) {
            throw new ResourceNotFoundException("User", callerId);
        }

        String directKey = ChatRoom.directKeyOf(callerId, otherUserId);
        ChatRoom room = chatRoomRepository.findByCollegeIdAndDirectKey(collegeId, directKey)
                .orElseGet(() -> createDirectRoom(collegeId, directKey, callerId, otherUserId));

        return chatMapper.toRoomResponse(room,
                chatParticipantRepository.findUserIdsByRoom(collegeId, room.getId()));
    }

    /**
     * Returns the classroom's room, creating it on first use, and mirrors the
     * classroom roster onto its participants.
     */
    @Transactional
    public ChatRoomResponse getOrCreateClassroomRoom(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);

        UUID collegeId = tenantGuard.requireCollegeId();
        ClassroomRef classroom = classroomAccessService.requireClassroom(classroomId);
        classroomAccessService.assertMember(classroomId);

        ChatRoom room = chatRoomRepository.findByCollegeIdAndClassroomId(collegeId, classroomId)
                .orElseGet(() -> chatRoomRepository.save(newRoom(collegeId, ChatRoomType.CLASSROOM,
                        classroom.name(), classroomId, ChatPermissionMode.EVERYONE, null)));

        List<String> participants = syncClassroomParticipants(room, collegeId, classroomId);
        return chatMapper.toRoomResponse(room, participants);
    }

    /**
     * Authoritative send path for both transports.
     * <p>
     * The order of the checks is part of the contract: room existence inside the
     * caller's college is resolved before anything else so a foreign room is
     * indistinguishable from a missing one; participation is then established
     * before any classroom lookup, so a non-participant never learns which
     * classroom a room belongs to; the classroom's own capability is applied
     * before the room's mode, because a teacher may narrow a room further than
     * the classroom does but may never widen it.
     */
    @Transactional
    public ChatMessageResponse sendMessage(UUID chatRoomId,
                                           String senderUserId,
                                           UUID senderCollegeId,
                                           String content,
                                           MessageType messageType,
                                           UUID fileId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);

        ChatRoom room = chatRoomRepository.findByIdAndCollegeId(chatRoomId, senderCollegeId)
                .orElseThrow(() -> new ResourceNotFoundException(ROOM, chatRoomId));

        if (!chatParticipantRepository.isParticipant(senderCollegeId, chatRoomId, senderUserId)) {
            throw new ForbiddenException("You are not a participant of this chat room");
        }

        if (room.isClassroomRoom()) {
            classroomAccessService.assertPermission(room.getClassroomId(),
                    ClassroomPermissionType.SEND_MESSAGE);
        }

        assertRoomModeAllowsPosting(room, senderUserId);
        validatePayload(messageType, content, fileId);

        ChatMessage draft = ChatMessage.builder()
                .chatRoom(room)
                .senderUserId(senderUserId)
                .content(content)
                .messageType(messageType)
                .fileId(fileId)
                .deleted(false)
                .build();
        draft.setCollegeId(senderCollegeId);
        ChatMessage message = chatMessageRepository.save(draft);

        List<String> recipients = chatParticipantRepository
                .findUserIdsByRoom(senderCollegeId, chatRoomId).stream()
                .filter(userId -> !userId.equals(senderUserId))
                .toList();

        eventPublisher.publishEvent(new ChatMessageSentEvent(
                senderCollegeId,
                chatRoomId,
                message.getId(),
                senderUserId,
                preview(messageType, content),
                recipients,
                Instant.now()));

        ChatMessageResponse response = chatMapper.toMessageResponse(message);
        messagingTemplate.convertAndSend(WebSocketDestinations.chatRoomTopic(chatRoomId), response);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> history(UUID chatRoomId, int page, int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();
        requireParticipation(chatRoomId, collegeId, callerId);

        // Newest first, with the id as tie-breaker so messages written in the
        // same instant cannot swap places between pages.
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<ChatMessage> messages = chatMessageRepository.findHistory(collegeId, chatRoomId, pageable);
        return PageResponse.from(messages, chatMapper::toMessageResponse);
    }

    @Transactional
    public void markAsRead(UUID chatRoomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        chatRoomRepository.findByIdAndCollegeId(chatRoomId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(ROOM, chatRoomId));

        if (chatParticipantRepository.markRead(collegeId, chatRoomId, callerId, LocalDateTime.now()) == 0) {
            throw new ForbiddenException("You are not a participant of this chat room");
        }
    }

    @Transactional(readOnly = true)
    public List<ChatUnreadCountResponse> unreadCounts() {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);
        return chatParticipantRepository.unreadCounts(
                tenantGuard.requireCollegeId(), tenantGuard.requireUserId());
    }

    @Transactional
    public ChatRoomResponse updatePermissionMode(UUID chatRoomId, ChatPermissionMode mode) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);

        UUID collegeId = tenantGuard.requireCollegeId();
        ChatRoom room = chatRoomRepository.findByIdAndCollegeId(chatRoomId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(ROOM, chatRoomId));

        if (!room.isClassroomRoom()) {
            throw new BusinessException("Only classroom chat rooms have a configurable permission mode");
        }
        classroomAccessService.assertTeacher(room.getClassroomId());

        room.setPermissionMode(mode);
        chatRoomRepository.save(room);

        return chatMapper.toRoomResponse(room,
                chatParticipantRepository.findUserIdsByRoom(collegeId, chatRoomId));
    }

    /** Soft-deletes a message; allowed for its sender and for classroom teachers. */
    @Transactional
    public void deleteMessage(UUID messageId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);

        UUID collegeId = tenantGuard.requireCollegeId();
        String callerId = tenantGuard.requireUserId();

        ChatMessage message = chatMessageRepository.findByIdAndCollegeId(messageId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(MESSAGE, messageId));

        ChatRoom room = message.getChatRoom();
        boolean isSender = callerId.equals(message.getSenderUserId());
        boolean isModerator = room.isClassroomRoom()
                && classroomAccessService.isTeacher(room.getClassroomId(), callerId);

        if (!isSender && !isModerator) {
            throw new ForbiddenException("Only the sender or a classroom teacher can delete this message");
        }

        message.setDeleted(true);
        chatMessageRepository.save(message);

        // Live clients are told to drop the message, without echoing its content back.
        messagingTemplate.convertAndSend(WebSocketDestinations.chatRoomTopic(room.getId()),
                new ChatMessageResponse(message.getId(), room.getId(), message.getSenderUserId(),
                        null, message.getMessageType(), null, true, message.getCreatedAt()));
    }

    /**
     * Confirms room membership and announces presence. Used by the STOMP join
     * frame once a client has subscribed to the room topic.
     */
    @Transactional
    public ChatRoomResponse joinRoom(UUID chatRoomId, String userId, UUID collegeId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CHAT);

        ChatRoom room = chatRoomRepository.findByIdAndCollegeId(chatRoomId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(ROOM, chatRoomId));

        if (chatParticipantRepository.markRead(collegeId, chatRoomId, userId, LocalDateTime.now()) == 0) {
            throw new ForbiddenException("You are not a participant of this chat room");
        }

        messagingTemplate.convertAndSend(WebSocketDestinations.chatRoomTopic(chatRoomId),
                new ChatPresenceEvent(chatRoomId, userId, Instant.now()));

        return chatMapper.toRoomResponse(room,
                chatParticipantRepository.findUserIdsByRoom(collegeId, chatRoomId));
    }

    private ChatRoom createDirectRoom(UUID collegeId, String directKey, String callerId, String otherUserId) {
        ChatRoom room = chatRoomRepository.save(
                newRoom(collegeId, ChatRoomType.DIRECT, null, null, ChatPermissionMode.EVERYONE, directKey));

        chatParticipantRepository.saveAll(List.of(
                newParticipant(room, collegeId, callerId),
                newParticipant(room, collegeId, otherUserId)));

        return room;
    }

    private ChatRoom newRoom(UUID collegeId,
                             ChatRoomType roomType,
                             String name,
                             UUID classroomId,
                             ChatPermissionMode permissionMode,
                             String directKey) {
        ChatRoom room = ChatRoom.builder()
                .name(name)
                .roomType(roomType)
                .classroomId(classroomId)
                .permissionMode(permissionMode)
                .directKey(directKey)
                .build();
        room.setCollegeId(collegeId);
        return room;
    }

    private ChatParticipant newParticipant(ChatRoom room, UUID collegeId, String userId) {
        ChatParticipant participant = ChatParticipant.builder()
                .chatRoom(room)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();
        participant.setCollegeId(collegeId);
        return participant;
    }

    /**
     * Brings the room's participants in line with the classroom roster in two
     * queries. Removals matter as much as additions: the STOMP subscription rule
     * authorizes on participant rows, so a stale row would keep a former member
     * subscribed to the room.
     */
    private List<String> syncClassroomParticipants(ChatRoom room, UUID collegeId, UUID classroomId) {
        Set<String> members = new LinkedHashSet<>(classroomAccessService.memberUserIds(classroomId));
        Set<String> existing = new HashSet<>(
                chatParticipantRepository.findUserIdsByRoom(collegeId, room.getId()));

        List<ChatParticipant> added = members.stream()
                .filter(userId -> !existing.contains(userId))
                .map(userId -> newParticipant(room, collegeId, userId))
                .toList();
        if (!added.isEmpty()) {
            chatParticipantRepository.saveAll(added);
        }

        List<String> removed = existing.stream()
                .filter(userId -> !members.contains(userId))
                .toList();
        if (!removed.isEmpty()) {
            chatParticipantRepository.deleteByRoomAndUserIds(collegeId, room.getId(), removed);
        }

        return List.copyOf(members);
    }

    private void requireParticipation(UUID chatRoomId, UUID collegeId, String userId) {
        chatRoomRepository.findByIdAndCollegeId(chatRoomId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(ROOM, chatRoomId));

        if (!chatParticipantRepository.isParticipant(collegeId, chatRoomId, userId)) {
            throw new ForbiddenException("You are not a participant of this chat room");
        }
    }

    private void assertRoomModeAllowsPosting(ChatRoom room, String senderUserId) {
        switch (room.getPermissionMode()) {
            case EVERYONE -> {
                // Participation, already established, is the only requirement.
            }
            case READ_ONLY -> throw new ForbiddenException("This chat room is read-only");
            case DISABLED -> throw new ForbiddenException("Chat is disabled for this room");
            case TEACHERS_ONLY -> {
                if (room.getClassroomId() == null
                        || !classroomAccessService.isTeacher(room.getClassroomId(), senderUserId)) {
                    throw new ForbiddenException("Only teachers can post in this chat room");
                }
            }
        }
    }

    private void validatePayload(MessageType messageType, String content, UUID fileId) {
        if (messageType == MessageType.SYSTEM) {
            throw new BusinessException("SYSTEM messages are generated by the server");
        }
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("Message content exceeds " + MAX_CONTENT_LENGTH + " characters");
        }
        if (messageType == MessageType.TEXT && (content == null || content.isBlank())) {
            throw new BusinessException("A text message needs content");
        }
        if ((messageType == MessageType.IMAGE || messageType == MessageType.FILE) && fileId == null) {
            throw new BusinessException("A " + messageType + " message needs a fileId");
        }
    }

    private String preview(MessageType messageType, String content) {
        if (messageType != MessageType.TEXT || content == null) {
            return "[" + messageType + "]";
        }
        return content.length() <= PREVIEW_LENGTH ? content : content.substring(0, PREVIEW_LENGTH) + "...";
    }
}
