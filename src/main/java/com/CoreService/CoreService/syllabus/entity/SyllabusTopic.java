package com.CoreService.CoreService.syllabus.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "syllabus_topics", indexes = {
        @Index(name = "idx_syllabus_topics_college_id", columnList = "college_id"),
        @Index(name = "idx_syllabus_topics_unit_id", columnList = "syllabus_unit_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SyllabusTopic extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "syllabus_unit_id", nullable = false)
    private SyllabusUnit unit;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 64)
    private String completedBy;
}
