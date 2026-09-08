package com.CoreService.CoreService.classroom.controller;

import com.CoreService.CoreService.classroom.dto.AddClassroomMembersRequest;
import com.CoreService.CoreService.classroom.dto.ClassroomDetailResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomMemberResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomPermissionResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomResponse;
import com.CoreService.CoreService.classroom.dto.CreateClassroomRequest;
import com.CoreService.CoreService.classroom.dto.UpdateClassroomPermissionRequest;
import com.CoreService.CoreService.classroom.dto.UpdateClassroomRequest;
import com.CoreService.CoreService.classroom.service.ClassroomService;
import com.CoreService.CoreService.common.response.ApiResponse;
import com.CoreService.CoreService.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;

    @PostMapping
    @PreAuthorize("hasAuthority('CLASSROOM_CREATE')")
    public ApiResponse<ClassroomDetailResponse> createClassroom(
            @Valid @RequestBody CreateClassroomRequest request) {

        return ApiResponse.success("Classroom created", classroomService.createClassroom(request));
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<ClassroomResponse>> myClassrooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean archived) {

        return ApiResponse.success(classroomService.myClassrooms(page, size, archived));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLASSROOM_VIEW')")
    public ApiResponse<PageResponse<ClassroomResponse>> allClassrooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean archived) {

        return ApiResponse.success(classroomService.allClassrooms(page, size, archived));
    }

    @GetMapping("/{classroomId}")
    public ApiResponse<ClassroomDetailResponse> getClassroom(@PathVariable UUID classroomId) {
        return ApiResponse.success(classroomService.getClassroom(classroomId));
    }

    @PutMapping("/{classroomId}")
    @PreAuthorize("hasAuthority('CLASSROOM_UPDATE')")
    public ApiResponse<ClassroomDetailResponse> updateClassroom(
            @PathVariable UUID classroomId,
            @Valid @RequestBody UpdateClassroomRequest request) {

        return ApiResponse.success("Classroom updated", classroomService.updateClassroom(classroomId, request));
    }

    @PostMapping("/{classroomId}/archive")
    @PreAuthorize("hasAuthority('CLASSROOM_UPDATE')")
    public ApiResponse<Void> archiveClassroom(@PathVariable UUID classroomId) {
        classroomService.archiveClassroom(classroomId);
        return ApiResponse.message("Classroom archived");
    }

    @PostMapping("/{classroomId}/unarchive")
    @PreAuthorize("hasAuthority('CLASSROOM_UPDATE')")
    public ApiResponse<Void> unarchiveClassroom(@PathVariable UUID classroomId) {
        classroomService.unarchiveClassroom(classroomId);
        return ApiResponse.message("Classroom restored");
    }

    @DeleteMapping("/{classroomId}")
    @PreAuthorize("hasAuthority('CLASSROOM_DELETE')")
    public ApiResponse<Void> deleteClassroom(@PathVariable UUID classroomId) {
        classroomService.deleteClassroom(classroomId);
        return ApiResponse.message("Classroom deleted");
    }

    @GetMapping("/{classroomId}/members")
    public ApiResponse<List<ClassroomMemberResponse>> listMembers(@PathVariable UUID classroomId) {
        return ApiResponse.success(classroomService.listMembers(classroomId));
    }

    @PostMapping("/{classroomId}/students")
    @PreAuthorize("hasAuthority('CLASSROOM_MEMBER_MANAGE')")
    public ApiResponse<List<ClassroomMemberResponse>> addStudents(
            @PathVariable UUID classroomId,
            @Valid @RequestBody AddClassroomMembersRequest request) {

        return ApiResponse.success("Students added",
                classroomService.addStudents(classroomId, request.userIds()));
    }

    @PostMapping("/{classroomId}/teachers")
    @PreAuthorize("hasAuthority('CLASSROOM_MEMBER_MANAGE')")
    public ApiResponse<List<ClassroomMemberResponse>> addCoTeachers(
            @PathVariable UUID classroomId,
            @Valid @RequestBody AddClassroomMembersRequest request) {

        return ApiResponse.success("Co-teachers added",
                classroomService.addCoTeachers(classroomId, request.userIds()));
    }

    @DeleteMapping("/{classroomId}/members/{userId}")
    @PreAuthorize("hasAuthority('CLASSROOM_MEMBER_MANAGE')")
    public ApiResponse<Void> removeMember(@PathVariable UUID classroomId,
                                          @PathVariable String userId) {

        classroomService.removeMember(classroomId, userId);
        return ApiResponse.message("Member removed");
    }

    @GetMapping("/{classroomId}/permissions")
    public ApiResponse<List<ClassroomPermissionResponse>> getPermissions(@PathVariable UUID classroomId) {
        return ApiResponse.success(classroomService.getPermissions(classroomId));
    }

    @PutMapping("/{classroomId}/permissions")
    @PreAuthorize("hasAuthority('CLASSROOM_PERMISSION_MANAGE')")
    public ApiResponse<ClassroomPermissionResponse> updatePermission(
            @PathVariable UUID classroomId,
            @Valid @RequestBody UpdateClassroomPermissionRequest request) {

        return ApiResponse.success("Classroom permission updated",
                classroomService.updatePermission(classroomId, request.permissionType(), request.mode()));
    }
}
