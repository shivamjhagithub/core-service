package com.CoreService.CoreService.syllabus.service;

import com.CoreService.CoreService.academic.service.AcademicLookupService;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.syllabus.dto.SyllabusDetailResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusProgressResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusRequest;
import com.CoreService.CoreService.syllabus.dto.SyllabusResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusTopicRequest;
import com.CoreService.CoreService.syllabus.dto.SyllabusTopicResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusUnitRequest;
import com.CoreService.CoreService.syllabus.dto.SyllabusUnitResponse;
import com.CoreService.CoreService.syllabus.entity.Syllabus;
import com.CoreService.CoreService.syllabus.entity.SyllabusTopic;
import com.CoreService.CoreService.syllabus.entity.SyllabusUnit;
import com.CoreService.CoreService.syllabus.event.SyllabusTopicCompletedEvent;
import com.CoreService.CoreService.syllabus.mapper.SyllabusMapper;
import com.CoreService.CoreService.syllabus.repository.SyllabusRepository;
import com.CoreService.CoreService.syllabus.repository.SyllabusSpecifications;
import com.CoreService.CoreService.syllabus.repository.SyllabusTopicRepository;
import com.CoreService.CoreService.syllabus.repository.SyllabusUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SyllabusService {

    private static final String SYLLABUS = "Syllabus";
    private static final String UNIT = "Syllabus unit";
    private static final String TOPIC = "Syllabus topic";
    private static final int MAX_PAGE_SIZE = 100;

    private final SyllabusRepository syllabusRepository;
    private final SyllabusUnitRepository syllabusUnitRepository;
    private final SyllabusTopicRepository syllabusTopicRepository;
    private final SyllabusMapper syllabusMapper;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;
    private final ClassroomAccessService classroomAccessService;
    private final AcademicLookupService academicLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public SyllabusResponse create(SyllabusRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        validateReferences(request.subjectId(), request.classroomId());

        Syllabus syllabus = new Syllabus();
        syllabus.setCollegeId(collegeId);
        applyCoreFields(syllabus, request);
        syllabus.setPublished(Boolean.TRUE.equals(request.published()));

        return syllabusMapper.toResponse(syllabusRepository.save(syllabus));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public SyllabusResponse update(UUID syllabusId, SyllabusRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        Syllabus syllabus = requireSyllabus(collegeId, syllabusId);
        validateReferences(request.subjectId(), request.classroomId());

        applyCoreFields(syllabus, request);
        if (request.published() != null) {
            syllabus.setPublished(request.published());
        }

        return syllabusMapper.toResponse(syllabusRepository.save(syllabus));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public void delete(UUID syllabusId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        Syllabus syllabus = requireSyllabus(collegeId, syllabusId);

        syllabusTopicRepository.deleteBySyllabus(collegeId, syllabusId);
        syllabusUnitRepository.deleteBySyllabus(collegeId, syllabusId);
        syllabusRepository.delete(syllabus);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SYLLABUS_VIEW')")
    public PageResponse<SyllabusResponse> list(UUID subjectId,
                                               UUID classroomId,
                                               Boolean published,
                                               int page,
                                               int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        if (classroomId != null) {
            assertClassroomReadable(classroomId);
        }

        Page<Syllabus> result = syllabusRepository.findAll(
                SyllabusSpecifications.filter(collegeId, subjectId, classroomId, published, visibleClassroomIds()),
                pageRequest(page, size, Sort.by(Sort.Direction.ASC, "title")));

        return PageResponse.from(result, syllabusMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SYLLABUS_VIEW')")
    public SyllabusDetailResponse detail(UUID syllabusId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        Syllabus syllabus = requireSyllabus(collegeId, syllabusId);
        assertReadable(syllabus);

        List<SyllabusUnit> units = syllabusUnitRepository.findBySyllabus(collegeId, syllabusId);
        List<SyllabusTopic> topics = syllabusTopicRepository.findBySyllabus(collegeId, syllabusId);
        SyllabusProgressResponse progress = syllabusMapper.toProgress(
                syllabusId, syllabusTopicRepository.aggregateProgress(collegeId, syllabusId));

        String subjectName = syllabus.getSubjectId() == null
                ? null
                : academicLookupService.subjectName(syllabus.getSubjectId());

        return syllabusMapper.toDetailResponse(syllabus, subjectName, units, topics, progress);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SYLLABUS_VIEW')")
    public SyllabusProgressResponse progress(UUID syllabusId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        assertReadable(requireSyllabus(collegeId, syllabusId));

        return syllabusMapper.toProgress(syllabusId,
                syllabusTopicRepository.aggregateProgress(collegeId, syllabusId));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public SyllabusUnitResponse addUnit(UUID syllabusId, SyllabusUnitRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        Syllabus syllabus = requireSyllabus(collegeId, syllabusId);

        SyllabusUnit unit = new SyllabusUnit();
        unit.setCollegeId(collegeId);
        unit.setSyllabus(syllabus);
        unit.setTitle(request.title());
        unit.setDescription(request.description());
        unit.setOrderIndex(request.orderIndex());

        return syllabusMapper.toUnitResponse(syllabusUnitRepository.save(unit), syllabusId, List.of());
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public SyllabusUnitResponse updateUnit(UUID unitId, SyllabusUnitRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        SyllabusUnit unit = requireUnit(collegeId, unitId);

        unit.setTitle(request.title());
        unit.setDescription(request.description());
        unit.setOrderIndex(request.orderIndex());
        SyllabusUnit saved = syllabusUnitRepository.save(unit);

        List<SyllabusTopicResponse> topics = syllabusTopicRepository.findByUnit(collegeId, unitId).stream()
                .map(topic -> syllabusMapper.toTopicResponse(topic, unitId))
                .toList();

        return syllabusMapper.toUnitResponse(saved, saved.getSyllabus().getId(), topics);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public void deleteUnit(UUID unitId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        SyllabusUnit unit = requireUnit(collegeId, unitId);

        syllabusTopicRepository.deleteByUnit(collegeId, unitId);
        syllabusUnitRepository.delete(unit);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public SyllabusTopicResponse addTopic(UUID unitId, SyllabusTopicRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        SyllabusUnit unit = requireUnit(collegeId, unitId);

        SyllabusTopic topic = new SyllabusTopic();
        topic.setCollegeId(collegeId);
        topic.setUnit(unit);
        topic.setTitle(request.title());
        topic.setDescription(request.description());
        topic.setOrderIndex(request.orderIndex());

        return syllabusMapper.toTopicResponse(syllabusTopicRepository.save(topic), unitId);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public SyllabusTopicResponse updateTopic(UUID topicId, SyllabusTopicRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        SyllabusTopic topic = requireTopic(collegeId, topicId);

        topic.setTitle(request.title());
        topic.setDescription(request.description());
        topic.setOrderIndex(request.orderIndex());

        return syllabusMapper.toTopicResponse(syllabusTopicRepository.save(topic), topic.getUnit().getId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public void deleteTopic(UUID topicId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        syllabusTopicRepository.delete(requireTopic(collegeId, topicId));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    public SyllabusTopicResponse markTopicCompleted(UUID topicId, boolean completed) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.SYLLABUS);
        UUID collegeId = tenantGuard.requireCollegeId();
        SyllabusTopic topic = requireTopic(collegeId, topicId);

        topic.setCompleted(completed);
        topic.setCompletedAt(completed ? LocalDateTime.now() : null);
        topic.setCompletedBy(completed ? tenantGuard.requireUserId() : null);
        SyllabusTopic saved = syllabusTopicRepository.save(topic);

        if (completed) {
            eventPublisher.publishEvent(new SyllabusTopicCompletedEvent(
                    collegeId,
                    saved.getUnit().getSyllabus().getId(),
                    saved.getId(),
                    saved.getTitle(),
                    Instant.now()));
        }

        return syllabusMapper.toTopicResponse(saved, saved.getUnit().getId());
    }

    private void applyCoreFields(Syllabus syllabus, SyllabusRequest request) {
        syllabus.setTitle(request.title());
        syllabus.setDescription(request.description());
        syllabus.setSubjectId(request.subjectId());
        syllabus.setClassroomId(request.classroomId());
    }

    private void validateReferences(UUID subjectId, UUID classroomId) {
        if (subjectId != null) {
            academicLookupService.requireSubject(subjectId);
        }
        if (classroomId != null) {
            classroomAccessService.requireClassroom(classroomId);
        }
    }

    private Syllabus requireSyllabus(UUID collegeId, UUID syllabusId) {
        return syllabusRepository.findByIdAndCollegeId(syllabusId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(SYLLABUS, syllabusId));
    }

    private SyllabusUnit requireUnit(UUID collegeId, UUID unitId) {
        return syllabusUnitRepository.findWithSyllabusByIdAndCollegeId(unitId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(UNIT, unitId));
    }

    private SyllabusTopic requireTopic(UUID collegeId, UUID topicId) {
        return syllabusTopicRepository.findWithUnitByIdAndCollegeId(topicId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(TOPIC, topicId));
    }

    private void assertReadable(Syllabus syllabus) {
        if (syllabus.getClassroomId() != null) {
            assertClassroomReadable(syllabus.getClassroomId());
        }
    }

    private void assertClassroomReadable(UUID classroomId) {
        if (tenantGuard.isMainAdmin()) {
            return;
        }
        if (classroomAccessService.isTeacher(classroomId, tenantGuard.requireUserId())) {
            return;
        }
        classroomAccessService.assertMember(classroomId);
    }

    /**
     * {@code null} means "no classroom restriction"; see
     * {@link SyllabusSpecifications#filter}.
     */
    private Collection<UUID> visibleClassroomIds() {
        return tenantGuard.isMainAdmin()
                ? null
                : classroomAccessService.classroomIdsOfUser(tenantGuard.requireUserId());
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }
}
