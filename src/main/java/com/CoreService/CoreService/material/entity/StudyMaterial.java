package com.CoreService.CoreService.material.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import com.CoreService.CoreService.material.enums.MaterialType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * A material is backed by exactly one source: an uploaded file or an external
 * link. When a classroom is set it also becomes the read-access scope.
 */
@Entity
@Table(name = "study_materials", indexes = {
        @Index(name = "idx_study_materials_college_id", columnList = "college_id"),
        @Index(name = "idx_study_materials_classroom_id", columnList = "classroom_id"),
        @Index(name = "idx_study_materials_subject_id", columnList = "subject_id")
})
@Getter
@Setter
@NoArgsConstructor
public class StudyMaterial extends TenantAwareEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "material_type", nullable = false, length = 20)
    private MaterialType materialType;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "link_url", length = 2000)
    private String linkUrl;

    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "topic_id")
    private UUID topicId;

    @Column(name = "uploaded_by", nullable = false, length = 64)
    private String uploadedBy;
}
