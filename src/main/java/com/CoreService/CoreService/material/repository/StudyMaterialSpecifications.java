package com.CoreService.CoreService.material.repository;

import com.CoreService.CoreService.material.entity.StudyMaterial;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class StudyMaterialSpecifications {

    private StudyMaterialSpecifications() {
    }

    /**
     * @param visibleClassroomIds classrooms the caller belongs to; an empty
     *                            collection limits the result to college-wide
     *                            materials, {@code null} lifts the restriction
     *                            altogether (main admin)
     */
    public static Specification<StudyMaterial> filter(UUID collegeId,
                                                      UUID classroomId,
                                                      UUID subjectId,
                                                      UUID topicId,
                                                      Collection<UUID> visibleClassroomIds) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("collegeId"), collegeId));

            if (subjectId != null) {
                predicates.add(builder.equal(root.get("subjectId"), subjectId));
            }
            if (topicId != null) {
                predicates.add(builder.equal(root.get("topicId"), topicId));
            }

            if (classroomId != null) {
                predicates.add(builder.equal(root.get("classroomId"), classroomId));
            } else if (visibleClassroomIds != null) {
                Predicate collegeWide = builder.isNull(root.get("classroomId"));
                predicates.add(visibleClassroomIds.isEmpty()
                        ? collegeWide
                        : builder.or(collegeWide, root.get("classroomId").in(visibleClassroomIds)));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
