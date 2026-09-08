package com.CoreService.CoreService.syllabus.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Course outline. A syllabus may hang off a subject, off a classroom, off both
 * or off neither; when a classroom is set it also becomes the read-access scope.
 */
@Entity
@Table(name = "syllabi", indexes = {
        @Index(name = "idx_syllabi_college_id", columnList = "college_id"),
        @Index(name = "idx_syllabi_subject_id", columnList = "subject_id"),
        @Index(name = "idx_syllabi_classroom_id", columnList = "classroom_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Syllabus extends TenantAwareEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "published", nullable = false)
    private boolean published;
}
