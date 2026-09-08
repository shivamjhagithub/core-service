package com.CoreService.CoreService.syllabus.controller;

import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusDetailResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusProgressResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusRequest;
import com.CoreService.CoreService.syllabus.dto.SyllabusResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusTopicRequest;
import com.CoreService.CoreService.syllabus.dto.SyllabusTopicResponse;
import com.CoreService.CoreService.syllabus.dto.SyllabusUnitRequest;
import com.CoreService.CoreService.syllabus.dto.SyllabusUnitResponse;
import com.CoreService.CoreService.syllabus.dto.TopicCompletionRequest;
import com.CoreService.CoreService.syllabus.service.SyllabusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/syllabus")
@RequiredArgsConstructor
public class SyllabusController {

    private final SyllabusService syllabusService;

    @PostMapping
    public ResponseEntity<ApiResponse<SyllabusResponse>> create(@Valid @RequestBody SyllabusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Syllabus created", syllabusService.create(request)));
    }

    @PutMapping("/{syllabusId}")
    public ApiResponse<SyllabusResponse> update(@PathVariable UUID syllabusId,
                                                @Valid @RequestBody SyllabusRequest request) {
        return ApiResponse.success("Syllabus updated", syllabusService.update(syllabusId, request));
    }

    @DeleteMapping("/{syllabusId}")
    public ApiResponse<Void> delete(@PathVariable UUID syllabusId) {
        syllabusService.delete(syllabusId);
        return ApiResponse.message("Syllabus deleted");
    }

    @GetMapping
    public ApiResponse<PageResponse<SyllabusResponse>> list(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID classroomId,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(syllabusService.list(subjectId, classroomId, published, page, size));
    }

    @GetMapping("/{syllabusId}")
    public ApiResponse<SyllabusDetailResponse> detail(@PathVariable UUID syllabusId) {
        return ApiResponse.success(syllabusService.detail(syllabusId));
    }

    @GetMapping("/{syllabusId}/progress")
    public ApiResponse<SyllabusProgressResponse> progress(@PathVariable UUID syllabusId) {
        return ApiResponse.success(syllabusService.progress(syllabusId));
    }

    @PostMapping("/{syllabusId}/units")
    public ResponseEntity<ApiResponse<SyllabusUnitResponse>> addUnit(
            @PathVariable UUID syllabusId,
            @Valid @RequestBody SyllabusUnitRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Unit added", syllabusService.addUnit(syllabusId, request)));
    }

    @PutMapping("/units/{unitId}")
    public ApiResponse<SyllabusUnitResponse> updateUnit(@PathVariable UUID unitId,
                                                        @Valid @RequestBody SyllabusUnitRequest request) {
        return ApiResponse.success("Unit updated", syllabusService.updateUnit(unitId, request));
    }

    @DeleteMapping("/units/{unitId}")
    public ApiResponse<Void> deleteUnit(@PathVariable UUID unitId) {
        syllabusService.deleteUnit(unitId);
        return ApiResponse.message("Unit deleted");
    }

    @PostMapping("/units/{unitId}/topics")
    public ResponseEntity<ApiResponse<SyllabusTopicResponse>> addTopic(
            @PathVariable UUID unitId,
            @Valid @RequestBody SyllabusTopicRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Topic added", syllabusService.addTopic(unitId, request)));
    }

    @PutMapping("/topics/{topicId}")
    public ApiResponse<SyllabusTopicResponse> updateTopic(@PathVariable UUID topicId,
                                                          @Valid @RequestBody SyllabusTopicRequest request) {
        return ApiResponse.success("Topic updated", syllabusService.updateTopic(topicId, request));
    }

    @DeleteMapping("/topics/{topicId}")
    public ApiResponse<Void> deleteTopic(@PathVariable UUID topicId) {
        syllabusService.deleteTopic(topicId);
        return ApiResponse.message("Topic deleted");
    }

    @PatchMapping("/topics/{topicId}/completion")
    public ApiResponse<SyllabusTopicResponse> markTopicCompleted(
            @PathVariable UUID topicId,
            @Valid @RequestBody TopicCompletionRequest request) {

        return ApiResponse.success("Topic completion updated",
                syllabusService.markTopicCompleted(topicId, request.completed()));
    }
}
