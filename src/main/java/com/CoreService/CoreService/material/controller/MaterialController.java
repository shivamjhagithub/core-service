package com.CoreService.CoreService.material.controller;

import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.material.dto.MaterialResponse;
import com.CoreService.CoreService.material.dto.MaterialUpdateRequest;
import com.CoreService.CoreService.material.dto.MaterialUploadRequest;
import com.CoreService.CoreService.material.service.StudyMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final StudyMaterialService studyMaterialService;

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialResponse>> upload(
            @Valid @RequestBody MaterialUploadRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Material published", studyMaterialService.upload(request)));
    }

    @PutMapping("/{materialId}")
    public ApiResponse<MaterialResponse> update(@PathVariable UUID materialId,
                                                @Valid @RequestBody MaterialUpdateRequest request) {
        return ApiResponse.success("Material updated", studyMaterialService.update(materialId, request));
    }

    @DeleteMapping("/{materialId}")
    public ApiResponse<Void> delete(@PathVariable UUID materialId) {
        studyMaterialService.delete(materialId);
        return ApiResponse.message("Material deleted");
    }

    @GetMapping
    public ApiResponse<PageResponse<MaterialResponse>> list(
            @RequestParam(required = false) UUID classroomId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(studyMaterialService.list(classroomId, subjectId, topicId, page, size));
    }

    @GetMapping("/{materialId}")
    public ApiResponse<MaterialResponse> detail(@PathVariable UUID materialId) {
        return ApiResponse.success(studyMaterialService.detail(materialId));
    }
}
