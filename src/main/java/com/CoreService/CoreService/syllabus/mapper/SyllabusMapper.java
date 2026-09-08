package com.CoreService.CoreService.syllabus.mapper;

import com.CoreService.CoreService.syllabus.dto.SyllabusDetailResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusProgressResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusProgressRow;
import com.CoreService.CoreService.syllabus.dto.SyllabusResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusTopicResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusUnitResponse;
import com.CoreService.CoreService.syllabus.entity.Syllabus;
import com.CoreService.CoreService.syllabus.entity.SyllabusTopic;
import com.CoreService.CoreService.syllabus.entity.SyllabusUnit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SyllabusMapper {

    public SyllabusResponse toResponse(Syllabus syllabus) {
        return new SyllabusResponse(
                syllabus.getId(),
                syllabus.getTitle(),
                syllabus.getDescription(),
                syllabus.getSubjectId(),
                syllabus.getClassroomId(),
                syllabus.isPublished(),
                syllabus.getCreatedAt(),
                syllabus.getUpdatedAt());
    }

    /**
     * Units and topics arrive as two flat, pre-ordered lists and are stitched
     * together here so the detail view costs a fixed number of queries.
     */
    public SyllabusDetailResponse toDetailResponse(Syllabus syllabus,
                                                   String subjectName,
                                                   List<SyllabusUnit> units,
                                                   List<SyllabusTopic> topics,
                                                   SyllabusProgressResponse progress) {

        Map<UUID, List<SyllabusTopicResponse>> topicsByUnit = new LinkedHashMap<>();
        for (SyllabusTopic topic : topics) {
            UUID unitId = topic.getUnit().getId();
            topicsByUnit.computeIfAbsent(unitId, key -> new ArrayList<>())
                    .add(toTopicResponse(topic, unitId));
        }

        List<SyllabusUnitResponse> unitResponses = units.stream()
                .map(unit -> toUnitResponse(unit, syllabus.getId(),
                        topicsByUnit.getOrDefault(unit.getId(), List.of())))
                .toList();

        return new SyllabusDetailResponse(
                syllabus.getId(),
                syllabus.getTitle(),
                syllabus.getDescription(),
                syllabus.getSubjectId(),
                subjectName,
                syllabus.getClassroomId(),
                syllabus.isPublished(),
                progress,
                unitResponses,
                syllabus.getCreatedAt(),
                syllabus.getUpdatedAt());
    }

    public SyllabusUnitResponse toUnitResponse(SyllabusUnit unit,
                                               UUID syllabusId,
                                               List<SyllabusTopicResponse> topics) {
        return new SyllabusUnitResponse(
                unit.getId(),
                syllabusId,
                unit.getTitle(),
                unit.getDescription(),
                unit.getOrderIndex(),
                topics);
    }

    public SyllabusTopicResponse toTopicResponse(SyllabusTopic topic, UUID unitId) {
        return new SyllabusTopicResponse(
                topic.getId(),
                unitId,
                topic.getTitle(),
                topic.getDescription(),
                topic.getOrderIndex(),
                topic.isCompleted(),
                topic.getCompletedAt(),
                topic.getCompletedBy());
    }

    public SyllabusProgressResponse toProgress(UUID syllabusId, SyllabusProgressRow row) {
        long total = row == null || row.totalTopics() == null ? 0L : row.totalTopics();
        long completed = row == null || row.completedTopics() == null ? 0L : row.completedTopics();
        double percentage = total == 0L ? 0d : Math.round(completed * 10_000d / total) / 100d;
        return new SyllabusProgressResponse(syllabusId, total, completed, percentage);
    }
}
