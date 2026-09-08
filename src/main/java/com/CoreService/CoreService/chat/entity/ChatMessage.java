package com.CoreService.CoreService.chat.entity;

import com.CoreService.CoreService.chat.enums.MessageType;
import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_college_id", columnList = "college_id"),
                @Index(name = "idx_chat_messages_chat_room_id", columnList = "chat_room_id"),
                @Index(name = "idx_chat_messages_created_at", columnList = "created_at"),
                @Index(name = "idx_chat_messages_room_created_at", columnList = "chat_room_id, created_at")
        })
public class ChatMessage extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_messages_room"))
    private ChatRoom chatRoom;

    @Column(name = "sender_user_id", nullable = false)
    private String senderUserId;

    @Column(name = "content", length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "message_type", nullable = false, length = 32)
    private MessageType messageType;

    /** Reference into the file module for IMAGE and FILE messages. */
    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
