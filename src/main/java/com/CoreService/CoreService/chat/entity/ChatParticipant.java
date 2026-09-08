package com.CoreService.CoreService.chat.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_participants_room_user",
                columnNames = {"chat_room_id", "user_id"}),
        indexes = {
                @Index(name = "idx_chat_participants_college_id", columnList = "college_id"),
                @Index(name = "idx_chat_participants_chat_room_id", columnList = "chat_room_id"),
                @Index(name = "idx_chat_participants_user_id", columnList = "user_id")
        })
public class ChatParticipant extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_participants_room"))
    private ChatRoom chatRoom;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    /** Watermark for unread counts; null means the participant never opened the room. */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;
}
