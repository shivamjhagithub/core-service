package com.CoreService.CoreService.student.entity;

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
 * Academic record of a student. The person lives in the users table and is
 * referenced by {@code userId}; department and program are held as plain
 * identifiers so the student module stays decoupled from academic entities.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "students",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_students_college_user",
                        columnNames = {"college_id", "user_id"}),
                @UniqueConstraint(
                        name = "uk_students_college_roll_number",
                        columnNames = {"college_id", "roll_number"}),
                @UniqueConstraint(
                        name = "uk_students_college_registration_number",
                        columnNames = {"college_id", "registration_number"})
        },
        indexes = {
                @Index(name = "idx_students_college_id", columnList = "college_id"),
                @Index(name = "idx_students_user_id", columnList = "user_id"),
                @Index(name = "idx_students_department_id", columnList = "department_id"),
                @Index(name = "idx_students_program_id", columnList = "program_id")
        })
public class Student extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(name = "roll_number", length = 64)
    private String rollNumber;

    @Column(name = "registration_number", length = 64)
    private String registrationNumber;

    @Column(name = "father_name", length = 150)
    private String fatherName;

    @Column(name = "blood_group", length = 8)
    private String bloodGroup;

    @Column(name = "semester")
    private Integer semester;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
