package com.CoreService.CoreService.material.mapper;

import com.CoreService.CoreService.file.dto.StoredFileRef;
import com.CoreService.CoreService.material.dto.MaterialResponse;
import com.CoreService.CoreService.material.entity.StudyMaterial;
import org.springframework.stereotype.Component;

@Component
public class StudyMaterialMapper {

    /**
     * @param fileRef {@code null} for link materials and for files that have
     *                since been deleted
     */
    public MaterialResponse toResponse(StudyMaterial material, StoredFileRef fileRef) {
        return new MaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getMaterialType(),
                material.getFileId(),
                fileRef == null ? null : fileRef.originalFileName(),
                fileRef == null ? null : fileRef.contentType(),
                fileRef == null ? null : Long.valueOf(fileRef.fileSize()),
                material.getLinkUrl(),
                material.getClassroomId(),
                material.getSubjectId(),
                material.getTopicId(),
                material.getUploadedBy(),
                material.getCreatedAt(),
                material.getUpdatedAt());
    }
}
