package com.CoreService.CoreService.material.service;

import com.CoreService.CoreService.academic.service.AcademicLookupService;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.ForbiddenException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.file.dto.StoredFileRef;
import com.CoreService.CoreService.file.service.FileLookupService;
import com.CoreService.CoreService.file.service.FileStorageService;
import com.CoreService.CoreService.material.dto.MaterialResponse;
import com.CoreService.CoreService.material.dto.MaterialUpdateRequest;
import com.CoreService.CoreService.material.dto.MaterialUploadRequest;
import com.CoreService.CoreService.material.entity.StudyMaterial;
import com.CoreService.CoreService.material.enums.MaterialType;
import com.CoreService.CoreService.material.event.MaterialUploadedEvent;
import com.CoreService.CoreService.material.mapper.StudyMaterialMapper;
import com.CoreService.CoreService.material.repository.StudyMaterialRepository;
import com.CoreService.CoreService.material.repository.StudyMaterialSpecifications;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyMaterialService {

    private static final String MATERIAL = "Study material";
    private static final int MAX_PAGE_SIZE = 100;

    private final StudyMaterialRepository studyMaterialRepository;
    private final StudyMaterialMapper studyMaterialMapper;
    private final FileStorageService fileStorageService;
    private final FileLookupService fileLookupService;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;
    private final ClassroomAccessService classroomAccessService;
    private final AcademicLookupService academicLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('MATERIAL_UPLOAD')")
    public MaterialResponse upload(MaterialUploadRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.STUDY_MATERIAL);
        UUID collegeId = tenantGuard.requireCollegeId();
        validateSource(request.materialType(), request.fileId(), request.linkUrl());

        if (request.classroomId() != null) {
            classroomAccessService.requireClassroom(request.classroomId());
            classroomAccessService.assertPermission(request.classroomId(), ClassroomPermissionType.UPLOAD_FILE);
        }
        if (request.subjectId() != null) {
            academicLookupService.requireSubject(request.subjectId());
        }

        // Rejects a file id that belongs to another college.
        StoredFileRef fileRef = request.fileId() == null ? null : fileStorageService.describe(request.fileId());

        StudyMaterial material = new StudyMaterial();
        material.setCollegeId(collegeId);
        material.setTitle(request.title());
        material.setDescription(request.description());
        material.setMaterialType(request.materialType());
        material.setFileId(request.fileId());
        material.setLinkUrl(request.linkUrl());
        material.setClassroomId(request.classroomId());
        material.setSubjectId(request.subjectId());
        material.setTopicId(request.topicId());
        material.setUploadedBy(tenantGuard.requireUserId());

        StudyMaterial saved = studyMaterialRepository.save(material);

        eventPublisher.publishEvent(new MaterialUploadedEvent(
                collegeId,
                saved.getId(),
                saved.getClassroomId(),
                saved.getTitle(),
                Instant.now()));

        return studyMaterialMapper.toResponse(saved, fileRef);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MATERIAL_UPLOAD')")
    public MaterialResponse update(UUID materialId, MaterialUpdateRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.STUDY_MATERIAL);
        UUID collegeId = tenantGuard.requireCollegeId();
        StudyMaterial material = requireMaterial(collegeId, materialId);
        assertManageable(material);
        validateSource(request.materialType(), request.fileId(), request.linkUrl());

        if (request.subjectId() != null) {
            academicLookupService.requireSubject(request.subjectId());
        }
        StoredFileRef fileRef = request.fileId() == null ? null : fileStorageService.describe(request.fileId());

        material.setTitle(request.title());
        material.setDescription(request.description());
        material.setMaterialType(request.materialType());
        material.setFileId(request.fileId());
        material.setLinkUrl(request.linkUrl());
        material.setSubjectId(request.subjectId());
        material.setTopicId(request.topicId());

        return studyMaterialMapper.toResponse(studyMaterialRepository.save(material), fileRef);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MATERIAL_DELETE')")
    public void delete(UUID materialId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.STUDY_MATERIAL);
        UUID collegeId = tenantGuard.requireCollegeId();
        StudyMaterial material = requireMaterial(collegeId, materialId);
        assertManageable(material);
        studyMaterialRepository.delete(material);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public PageResponse<MaterialResponse> list(UUID classroomId,
                                               UUID subjectId,
                                               UUID topicId,
                                               int page,
                                               int size) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.STUDY_MATERIAL);
        UUID collegeId = tenantGuard.requireCollegeId();
        if (classroomId != null) {
            assertClassroomReadable(classroomId);
        }

        Page<StudyMaterial> result = studyMaterialRepository.findAll(
                StudyMaterialSpecifications.filter(collegeId, classroomId, subjectId, topicId, visibleClassroomIds()),
                pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<UUID, StoredFileRef> fileRefs = resolveFiles(result.getContent());

        return PageResponse.from(result, material -> studyMaterialMapper.toResponse(
                material,
                material.getFileId() == null ? null : fileRefs.get(material.getFileId())));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public MaterialResponse detail(UUID materialId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.STUDY_MATERIAL);
        UUID collegeId = tenantGuard.requireCollegeId();
        StudyMaterial material = requireMaterial(collegeId, materialId);
        if (material.getClassroomId() != null) {
            assertClassroomReadable(material.getClassroomId());
        }
        return studyMaterialMapper.toResponse(material, fileRefOf(material.getFileId()));
    }

    private void validateSource(MaterialType materialType, UUID fileId, String linkUrl) {
        boolean hasFile = fileId != null;
        boolean hasLink = linkUrl != null && !linkUrl.isBlank();

        if (hasFile && hasLink) {
            throw new BusinessException("A material carries either a file or a link, never both");
        }
        if (!hasFile && !hasLink) {
            throw new BusinessException("A material must reference either a file or a link");
        }
        if (materialType == MaterialType.LINK && !hasLink) {
            throw new BusinessException("A link material requires a link URL");
        }
        if (materialType != MaterialType.LINK && !hasFile) {
            throw new BusinessException("A " + materialType + " material requires an uploaded file");
        }
    }

    private void assertManageable(StudyMaterial material) {
        if (tenantGuard.isMainAdmin()) {
            return;
        }
        String userId = tenantGuard.requireUserId();
        if (material.getUploadedBy().equals(userId)) {
            return;
        }
        if (material.getClassroomId() != null
                && classroomAccessService.isTeacher(material.getClassroomId(), userId)) {
            return;
        }
        throw new ForbiddenException("Only the uploader or a teacher of the classroom can change this material");
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
     * {@link StudyMaterialSpecifications#filter}.
     */
    private Collection<UUID> visibleClassroomIds() {
        return tenantGuard.isMainAdmin()
                ? null
                : classroomAccessService.classroomIdsOfUser(tenantGuard.requireUserId());
    }

    private Map<UUID, StoredFileRef> resolveFiles(List<StudyMaterial> materials) {
        List<UUID> fileIds = materials.stream()
                .map(StudyMaterial::getFileId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        return fileLookupService.describeAll(fileIds).stream()
                .collect(Collectors.toMap(StoredFileRef::fileId, Function.identity(), (first, second) -> first));
    }

    /** Tolerates a file that has been deleted since the material was created. */
    private StoredFileRef fileRefOf(UUID fileId) {
        if (fileId == null) {
            return null;
        }
        return fileLookupService.describeAll(List.of(fileId)).stream().findFirst().orElse(null);
    }

    private StudyMaterial requireMaterial(UUID collegeId, UUID materialId) {
        return studyMaterialRepository.findByIdAndCollegeId(materialId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(MATERIAL, materialId));
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }
}
