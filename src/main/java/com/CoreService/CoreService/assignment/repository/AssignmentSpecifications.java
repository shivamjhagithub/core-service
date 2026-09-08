package com.CoreService.CoreService.assignment.repository;

import com.CoreService.CoreService.assignment.entity.Assignment;
import com.CoreService.CoreService.assignment.enums.AssignmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class AssignmentSpecifications {

    private AssignmentSpecifications() {
    }

    /**
     * @param classroomIds must not be empty; callers resolve the caller's
     *                     classrooms first and short-circuit on an empty result
     * @param statuses     {@code null} or empty keeps every status
     */
    public static Specification<Assignment> filter(UUID collegeId,
                                                   Collection<UUID> classroomIds,
                                                   Collection<AssignmentStatus> statuses) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("collegeId"), collegeId));
            predicates.add(root.get("classroomId").in(classroomIds));
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
