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

@Entity
@Table(name = "syllabus_units", indexes = {
        @Index(name = "idx_syllabus_units_college_id", columnList = "college_id"),
        @Index(name = "idx_syllabus_units_syllabus_id", columnList = "syllabus_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SyllabusUnit extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "syllabus_id", nullable = false)
    private Syllabus syllabus;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "order_index")
    private Integer orderIndex;
}
