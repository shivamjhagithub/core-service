package com.CoreService.CoreService.teacher.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Employment record of a teacher. The person lives in the users table and is
 * referenced by {@code userId}; the department is a plain identifier so the
 * teacher module stays decoupled from academic entities.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "teachers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_teachers_college_user",
                        columnNames = {"college_id", "user_id"}),
                @UniqueConstraint(
                        name = "uk_teachers_college_employee_id",
                        columnNames = {"college_id", "employee_id"})
        },
        indexes = {
                @Index(name = "idx_teachers_college_id", columnList = "college_id"),
                @Index(name = "idx_teachers_user_id", columnList = "user_id"),
                @Index(name = "idx_teachers_department_id", columnList = "department_id")
        })
public class Teacher extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(name = "employee_id", length = 64)
    private String employeeId;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "qualification", length = 255)
    private String qualification;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
