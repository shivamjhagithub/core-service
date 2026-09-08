package com.CoreService.CoreService.chat.entity;

import com.CoreService.CoreService.chat.enums.ChatPermissionMode;
import com.CoreService.CoreService.chat.enums.ChatRoomType;
import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_rooms_college_direct_key",
                        columnNames = {"college_id", "direct_key"}),
                @UniqueConstraint(name = "uk_chat_rooms_college_classroom",
                        columnNames = {"college_id", "classroom_id"})
        },
        indexes = {
                @Index(name = "idx_chat_rooms_college_id", columnList = "college_id"),
                @Index(name = "idx_chat_rooms_classroom_id", columnList = "classroom_id")
        })
public class ChatRoom extends TenantAwareEntity {

    /** Null for DIRECT rooms, whose title is derived from the other participant. */
    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "room_type", nullable = false, length = 32)
    private ChatRoomType roomType;

    /** Set only for CLASSROOM rooms; a classroom has at most one chat room. */
    @Column(name = "classroom_id")
    private UUID classroomId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "permission_mode", nullable = false, length = 32)
    private ChatPermissionMode permissionMode;

    /**
     * The two participant ids of a DIRECT room, sorted and joined with '|'.
     * Unique per college, which is what makes opening a direct room idempotent
     * regardless of who initiates it.
     */
    @Column(name = "direct_key", length = 512)
    private String directKey;

    /** Canonical DIRECT room key: order-independent so both peers resolve the same room. */
    public static String directKeyOf(String firstUserId, String secondUserId) {
        return firstUserId.compareTo(secondUserId) <= 0
                ? firstUserId + "|" + secondUserId
                : secondUserId + "|" + firstUserId;
    }

    public boolean isClassroomRoom() {
        return roomType == ChatRoomType.CLASSROOM;
    }
}
