package com.CoreService.CoreService.classroom.entity;

import com.CoreService.CoreService.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "classrooms", indexes = {
        @Index(name = "idx_classrooms_college_id", columnList = "college_id"),
        @Index(name = "idx_classrooms_created_by", columnList = "created_by")
})
public class Classroom extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    /** Users table primary key of the teacher who opened the classroom. */
    @Column(name = "created_by", nullable = false, updatable = false, length = 64)
    private String createdBy;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "archived", nullable = false)
    private boolean archived;
}
