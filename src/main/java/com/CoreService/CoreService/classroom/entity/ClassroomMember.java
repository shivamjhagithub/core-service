package com.CoreService.CoreService.classroom.entity;

import com.CoreService.CoreService.classroom.enums.ClassroomMemberType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "classroom_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_classroom_members_classroom_user",
                columnNames = {"classroom_id", "user_id"}),
        indexes = {
                @Index(name = "idx_classroom_members_college_id", columnList = "college_id"),
                @Index(name = "idx_classroom_members_classroom_id", columnList = "classroom_id"),
                @Index(name = "idx_classroom_members_user_id", columnList = "user_id")
        })
public class ClassroomMember extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_classroom_members_classroom"))
    private Classroom classroom;

    @Column(name = "user_id", nullable = false, updatable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "member_type", nullable = false, length = 20)
    private ClassroomMemberType memberType;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
}
