package com.CoreService.CoreService.classroom.entity;

import com.CoreService.CoreService.classroom.enums.ClassroomPermissionMode;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
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

@Getter
@Setter
@Entity
@Table(name = "classroom_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_classroom_permissions_classroom_type",
                columnNames = {"classroom_id", "permission_type"}),
        indexes = {
                @Index(name = "idx_classroom_permissions_college_id", columnList = "college_id"),
                @Index(name = "idx_classroom_permissions_classroom_id", columnList = "classroom_id")
        })
public class ClassroomPermission extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_classroom_permissions_classroom"))
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "permission_type", nullable = false, updatable = false, length = 40)
    private ClassroomPermissionType permissionType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "mode", nullable = false, length = 20)
    private ClassroomPermissionMode mode;
}
