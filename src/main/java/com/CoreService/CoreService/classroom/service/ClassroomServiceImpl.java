package com.CoreService.CoreService.classroom.service;

import com.CoreService.CoreService.audit.service.AuditRecorder;
import com.CoreService.CoreService.classroom.dto.ClassroomDetailResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomMemberCount;
import com.CoreService.CoreService.classroom.dto.ClassroomMemberResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomPermissionResponse;
import com.CoreService.CoreService.classroom.dto.ClassroomRef;
import com.CoreService.CoreService.classroom.dto.ClassroomResponse;
import com.CoreService.CoreService.classroom.dto.CreateClassroomRequest;
import com.CoreService.CoreService.classroom.dto.UpdateClassroomRequest;
import com.CoreService.CoreService.classroom.entity.Classroom;
import com.CoreService.CoreService.classroom.entity.ClassroomMember;
import com.CoreService.CoreService.classroom.entity.ClassroomPermission;
import com.CoreService.CoreService.classroom.enums.ClassroomMemberType;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionMode;
import com.CoreService.CoreService.classroom.enums.ClassroomPermissionType;
import com.CoreService.CoreService.classroom.event.ClassroomCreatedEvent;
import com.CoreService.CoreService.classroom.event.ClassroomMemberAddedEvent;
import com.CoreService.CoreService.classroom.mapper.ClassroomMapper;
import com.CoreService.CoreService.classroom.repository.ClassroomMemberRepository;
import com.CoreService.CoreService.classroom.repository.ClassroomPermissionRepository;
import com.CoreService.CoreService.classroom.repository.ClassroomRepository;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.DuplicateResourceException;
import com.CoreService.CoreService.common.exception.ForbiddenException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.Permissions;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService, ClassroomAccessService {

    private static final String CLASSROOM = "Classroom";
    private static final String CLASSROOM_MEMBER = "Classroom member";

    /** A capability that was never configured is enforced as teachers-only. */
    private static final ClassroomPermissionMode FALLBACK_MODE = ClassroomPermissionMode.TEACHERS_ONLY;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final ClassroomPermissionRepository classroomPermissionRepository;
    private final ClassroomMapper classroomMapper;
    private final UserRepo userRepo;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher eventPublisher;

    // ------------------------------------------------------------------
    // Classroom lifecycle
    // ------------------------------------------------------------------

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_CREATE')")
    public ClassroomDetailResponse createClassroom(CreateClassroomRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        UUID collegeId = tenantGuard.requireCollegeId();
        String creatorUserId = tenantGuard.requireUserId();

        Classroom classroom = new Classroom();
        classroom.setCollegeId(collegeId);
        classroom.setName(request.name().trim());
        classroom.setDescription(request.description());
        classroom.setCreatedBy(creatorUserId);
        classroom.setActive(true);
        classroom.setArchived(false);

        Classroom saved = classroomRepository.save(classroom);

        classroomMemberRepository.save(newMember(saved, creatorUserId, ClassroomMemberType.TEACHER));
        classroomPermissionRepository.saveAll(Arrays.stream(ClassroomPermissionType.values())
                .map(type -> newPermission(saved, type, seededMode(type)))
                .toList());

        auditRecorder.record("CLASSROOM_CREATED", CLASSROOM, saved.getId());
        eventPublisher.publishEvent(new ClassroomCreatedEvent(collegeId,
                saved.getId(),
                saved.getName(),
                creatorUserId,
                Instant.now()));

        return detail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomDetailResponse getClassroom(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        Classroom classroom = loadClassroom(classroomId);
        assertReadAccess(classroom);
        return detail(classroom);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_UPDATE')")
    public ClassroomDetailResponse updateClassroom(UUID classroomId, UpdateClassroomRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        Classroom classroom = loadClassroom(classroomId);
        assertNotArchived(classroom);
        assertClassroomManager(classroom);

        classroom.setName(request.name().trim());
        classroom.setDescription(request.description());
        if (request.active() != null) {
            classroom.setActive(request.active());
        }

        Classroom saved = classroomRepository.save(classroom);
        auditRecorder.record("CLASSROOM_UPDATED", CLASSROOM, saved.getId());
        return detail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassroomResponse> myClassrooms(int page, int size, boolean archived) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();

        Page<Classroom> classrooms = classroomRepository
                .findMemberClassrooms(collegeId, userId, archived, pageRequest(page, size));
        return withMemberCounts(collegeId, classrooms);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLASSROOM_VIEW')")
    public PageResponse<ClassroomResponse> allClassrooms(int page, int size, boolean archived) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        UUID collegeId = tenantGuard.requireCollegeId();

        Page<Classroom> classrooms = classroomRepository
                .findAllByCollegeIdAndArchived(collegeId, archived, pageRequest(page, size));
        return withMemberCounts(collegeId, classrooms);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_UPDATE')")
    public void archiveClassroom(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        Classroom classroom = loadClassroom(classroomId);
        assertNotArchived(classroom);
        assertClassroomManager(classroom);

        classroom.setArchived(true);
        classroomRepository.save(classroom);
        auditRecorder.record("CLASSROOM_ARCHIVED", CLASSROOM, classroom.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_UPDATE')")
    public void unarchiveClassroom(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        Classroom classroom = loadClassroom(classroomId);
        assertClassroomManager(classroom);

        if (!classroom.isArchived()) {
            throw new BusinessException("Classroom is not archived");
        }

        classroom.setArchived(false);
        classroomRepository.save(classroom);
        auditRecorder.record("CLASSROOM_UNARCHIVED", CLASSROOM, classroom.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_DELETE')")
    public void deleteClassroom(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        UUID collegeId = tenantGuard.requireCollegeId();
        Classroom classroom = loadClassroom(classroomId);
        assertClassroomManager(classroom);

        classroomPermissionRepository.deleteAllOfClassroom(collegeId, classroomId);
        classroomMemberRepository.deleteAllOfClassroom(collegeId, classroomId);
        classroomRepository.delete(classroom);

        auditRecorder.record("CLASSROOM_DELETED", CLASSROOM, classroomId);
    }

    // ------------------------------------------------------------------
    // Membership
    // ------------------------------------------------------------------

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_MEMBER_MANAGE')")
    public List<ClassroomMemberResponse> addStudents(UUID classroomId, List<String> userIds) {
        return addMembers(classroomId, userIds, ClassroomMemberType.STUDENT);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_MEMBER_MANAGE')")
    public List<ClassroomMemberResponse> addCoTeachers(UUID classroomId, List<String> userIds) {
        return addMembers(classroomId, userIds, ClassroomMemberType.TEACHER);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_MEMBER_MANAGE')")
    public void removeMember(UUID classroomId, String userId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        UUID collegeId = tenantGuard.requireCollegeId();
        Classroom classroom = loadClassroom(classroomId);
        assertNotArchived(classroom);
        assertClassroomTeacher(classroom);

        ClassroomMember member = classroomMemberRepository
                .findByCollegeIdAndClassroom_IdAndUserId(collegeId, classroomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(CLASSROOM_MEMBER, userId));

        if (member.getMemberType() == ClassroomMemberType.TEACHER
                && classroomMemberRepository.countByCollegeIdAndClassroom_IdAndMemberType(
                collegeId, classroomId, ClassroomMemberType.TEACHER) <= 1) {
            throw new BusinessException("A classroom must keep at least one teacher");
        }

        classroomMemberRepository.delete(member);
        auditRecorder.record("CLASSROOM_MEMBER_REMOVED", CLASSROOM, classroomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomMemberResponse> listMembers(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        Classroom classroom = loadClassroom(classroomId);
        assertReadAccess(classroom);
        return memberResponses(classroom);
    }

    private List<ClassroomMemberResponse> addMembers(UUID classroomId,
                                                     List<String> userIds,
                                                     ClassroomMemberType memberType) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        UUID collegeId = tenantGuard.requireCollegeId();
        Classroom classroom = loadClassroom(classroomId);
        assertNotArchived(classroom);
        assertClassroomTeacher(classroom);

        List<String> requested = distinct(userIds);
        if (requested.isEmpty()) {
            throw new BusinessException("At least one user id is required");
        }

        Map<String, String> userNames = requireUsersInCollege(collegeId, requested);
        rejectExistingMembers(collegeId, classroomId, requested);

        List<ClassroomMember> members = requested.stream()
                .map(userId -> newMember(classroom, userId, memberType))
                .toList();
        classroomMemberRepository.saveAll(members);

        auditRecorder.record("CLASSROOM_MEMBERS_ADDED", CLASSROOM, classroomId);
        eventPublisher.publishEvent(new ClassroomMemberAddedEvent(collegeId,
                classroomId,
                classroom.getName(),
                requested,
                memberType,
                Instant.now()));

        return classroomMapper.toMemberResponses(members, userNames);
    }

    // ------------------------------------------------------------------
    // Classroom capabilities
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomPermissionResponse> getPermissions(UUID classroomId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        Classroom classroom = loadClassroom(classroomId);
        assertReadAccess(classroom);
        return effectivePermissions(classroom.getCollegeId(), classroom.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLASSROOM_PERMISSION_MANAGE')")
    public ClassroomPermissionResponse updatePermission(UUID classroomId,
                                                        ClassroomPermissionType permissionType,
                                                        ClassroomPermissionMode mode) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.CLASSROOM);
        UUID collegeId = tenantGuard.requireCollegeId();
        Classroom classroom = loadClassroom(classroomId);
        assertNotArchived(classroom);
        assertClassroomTeacher(classroom);

        ClassroomPermission permission = classroomPermissionRepository
                .findByCollegeIdAndClassroom_IdAndPermissionType(collegeId, classroomId, permissionType)
                .orElseGet(() -> newPermission(classroom, permissionType, mode));
        permission.setMode(mode);

        ClassroomPermission saved = classroomPermissionRepository.save(permission);
        auditRecorder.record("CLASSROOM_PERMISSION_UPDATED", CLASSROOM, classroomId);

        return classroomMapper.toPermissionResponse(saved.getPermissionType(), saved.getMode());
    }

    // ------------------------------------------------------------------
    // ClassroomAccessService - published to the other modules
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ClassroomRef requireClassroom(UUID classroomId) {
        return classroomMapper.toRef(loadClassroom(classroomId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(UUID classroomId, String userId) {
        return classroomMemberRepository.existsByCollegeIdAndClassroom_IdAndUserId(
                tenantGuard.requireCollegeId(), classroomId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeacher(UUID classroomId, String userId) {
        return classroomMemberRepository.existsByCollegeIdAndClassroom_IdAndUserIdAndMemberType(
                tenantGuard.requireCollegeId(), classroomId, userId, ClassroomMemberType.TEACHER);
    }

    /**
     * Runs on WebSocket threads, where no request-scoped tenant context exists,
     * so the college is supplied by the caller and never read from a ThreadLocal.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isMemberOf(UUID collegeId, UUID classroomId, String userId) {
        if (collegeId == null || classroomId == null || userId == null) {
            return false;
        }
        return classroomMemberRepository
                .existsByCollegeIdAndClassroom_IdAndUserId(collegeId, classroomId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertMember(UUID classroomId) {
        Classroom classroom = loadClassroom(classroomId);
        if (!classroomMemberRepository.existsByCollegeIdAndClassroom_IdAndUserId(
                classroom.getCollegeId(), classroom.getId(), tenantGuard.requireUserId())) {
            throw new ForbiddenException("You are not a member of this classroom");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertTeacher(UUID classroomId) {
        assertClassroomTeacher(loadClassroom(classroomId));
    }

    @Override
    @Transactional(readOnly = true)
    public void assertPermission(UUID classroomId, ClassroomPermissionType permission) {
        Classroom classroom = loadClassroom(classroomId);
        UUID collegeId = classroom.getCollegeId();

        ClassroomMember member = classroomMemberRepository
                .findByCollegeIdAndClassroom_IdAndUserId(collegeId, classroom.getId(), tenantGuard.requireUserId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this classroom"));

        ClassroomPermissionMode mode = classroomPermissionRepository
                .findByCollegeIdAndClassroom_IdAndPermissionType(collegeId, classroom.getId(), permission)
                .map(ClassroomPermission::getMode)
                .orElse(FALLBACK_MODE);

        if (mode == ClassroomPermissionMode.DISABLED) {
            throw new ForbiddenException(permission.name() + " is disabled in this classroom");
        }
        if (mode == ClassroomPermissionMode.TEACHERS_ONLY
                && member.getMemberType() != ClassroomMemberType.TEACHER) {
            throw new ForbiddenException(permission.name() + " is restricted to teachers of this classroom");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> memberUserIds(UUID classroomId) {
        Classroom classroom = loadClassroom(classroomId);
        return classroomMemberRepository.findMemberUserIds(classroom.getCollegeId(), classroom.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> studentUserIds(UUID classroomId) {
        Classroom classroom = loadClassroom(classroomId);
        return classroomMemberRepository.findMemberUserIdsByType(
                classroom.getCollegeId(), classroom.getId(), ClassroomMemberType.STUDENT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> teacherUserIds(UUID classroomId) {
        Classroom classroom = loadClassroom(classroomId);
        return classroomMemberRepository.findMemberUserIdsByType(
                classroom.getCollegeId(), classroom.getId(), ClassroomMemberType.TEACHER);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> classroomIdsOfUser(String userId) {
        return classroomMemberRepository.findClassroomIdsOfUser(tenantGuard.requireCollegeId(), userId);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private Classroom loadClassroom(UUID classroomId) {
        return classroomRepository.findByIdAndCollegeId(classroomId, tenantGuard.requireCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException(CLASSROOM, classroomId));
    }

    private void assertClassroomTeacher(Classroom classroom) {
        if (!classroomMemberRepository.existsByCollegeIdAndClassroom_IdAndUserIdAndMemberType(
                classroom.getCollegeId(),
                classroom.getId(),
                tenantGuard.requireUserId(),
                ClassroomMemberType.TEACHER)) {
            throw new ForbiddenException("Only a teacher of this classroom can perform this operation");
        }
    }

    private void assertClassroomManager(Classroom classroom) {
        if (tenantGuard.isMainAdmin() || isCollegeAdmin()) {
            return;
        }
        assertClassroomTeacher(classroom);
    }

    private void assertReadAccess(Classroom classroom) {
        if (tenantGuard.isMainAdmin() || isCollegeAdmin() || hasAuthority(Permissions.CLASSROOM_VIEW)) {
            return;
        }
        if (!classroomMemberRepository.existsByCollegeIdAndClassroom_IdAndUserId(
                classroom.getCollegeId(), classroom.getId(), tenantGuard.requireUserId())) {
            throw new ForbiddenException("You are not a member of this classroom");
        }
    }

    private void assertNotArchived(Classroom classroom) {
        if (classroom.isArchived()) {
            throw new BusinessException("Classroom is archived; restore it before making changes");
        }
    }

    private void rejectExistingMembers(UUID collegeId, UUID classroomId, List<String> userIds) {
        List<String> existing = classroomMemberRepository
                .findAllByCollegeIdAndClassroom_IdAndUserIdIn(collegeId, classroomId, userIds)
                .stream()
                .map(ClassroomMember::getUserId)
                .toList();

        if (!existing.isEmpty()) {
            throw new DuplicateResourceException(
                    "Already a member of this classroom: " + String.join(", ", existing));
        }
    }

    private Map<String, String> requireUsersInCollege(UUID collegeId, List<String> userIds) {
        Map<String, String> userNames = userNames(collegeId, userIds);

        List<String> unknown = userIds.stream()
                .filter(userId -> !userNames.containsKey(userId))
                .toList();
        if (!unknown.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Users not found in this college: " + String.join(", ", unknown));
        }
        return userNames;
    }

    private Map<String, String> userNames(UUID collegeId, Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepo.findAllById(userIds).stream()
                .filter(user -> collegeId.equals(user.getCollegeId()))
                .collect(Collectors.toMap(UserEntity::getUserId, UserEntity::getUserName));
    }

    private ClassroomDetailResponse detail(Classroom classroom) {
        return classroomMapper.toDetailResponse(classroom,
                memberResponses(classroom),
                effectivePermissions(classroom.getCollegeId(), classroom.getId()));
    }

    private List<ClassroomMemberResponse> memberResponses(Classroom classroom) {
        List<ClassroomMember> members = classroomMemberRepository
                .findAllByCollegeIdAndClassroom_IdOrderByJoinedAtAsc(
                        classroom.getCollegeId(), classroom.getId());

        Map<String, String> userNames = userNames(classroom.getCollegeId(),
                members.stream().map(ClassroomMember::getUserId).toList());

        return classroomMapper.toMemberResponses(members, userNames);
    }

    private List<ClassroomPermissionResponse> effectivePermissions(UUID collegeId, UUID classroomId) {
        Map<ClassroomPermissionType, ClassroomPermissionMode> configured =
                new EnumMap<>(ClassroomPermissionType.class);
        classroomPermissionRepository.findAllByCollegeIdAndClassroom_Id(collegeId, classroomId)
                .forEach(permission -> configured.put(permission.getPermissionType(), permission.getMode()));

        return Arrays.stream(ClassroomPermissionType.values())
                .map(type -> classroomMapper.toPermissionResponse(type,
                        configured.getOrDefault(type, FALLBACK_MODE)))
                .toList();
    }

    private PageResponse<ClassroomResponse> withMemberCounts(UUID collegeId, Page<Classroom> classrooms) {
        List<UUID> classroomIds = classrooms.getContent().stream().map(Classroom::getId).toList();

        Map<UUID, Long> memberCounts = classroomIds.isEmpty()
                ? Map.of()
                : classroomMemberRepository.countMembersByClassroom(collegeId, classroomIds).stream()
                .collect(Collectors.toMap(ClassroomMemberCount::classroomId, ClassroomMemberCount::memberCount));

        return PageResponse.from(classrooms,
                classroom -> classroomMapper.toResponse(classroom,
                        memberCounts.getOrDefault(classroom.getId(), 0L)));
    }

    private ClassroomMember newMember(Classroom classroom, String userId, ClassroomMemberType memberType) {
        ClassroomMember member = new ClassroomMember();
        member.setCollegeId(classroom.getCollegeId());
        member.setClassroom(classroom);
        member.setUserId(userId);
        member.setMemberType(memberType);
        member.setJoinedAt(LocalDateTime.now());
        return member;
    }

    private ClassroomPermission newPermission(Classroom classroom,
                                              ClassroomPermissionType permissionType,
                                              ClassroomPermissionMode mode) {
        ClassroomPermission permission = new ClassroomPermission();
        permission.setCollegeId(classroom.getCollegeId());
        permission.setClassroom(classroom);
        permission.setPermissionType(permissionType);
        permission.setMode(mode);
        return permission;
    }

    private static ClassroomPermissionMode seededMode(ClassroomPermissionType permissionType) {
        return switch (permissionType) {
            case SEND_MESSAGE -> ClassroomPermissionMode.EVERYONE;
            case UPLOAD_FILE, CREATE_MEETING, CREATE_POST -> ClassroomPermissionMode.TEACHERS_ONLY;
        };
    }

    private static List<String> distinct(List<String> userIds) {
        if (userIds == null) {
            return List.of();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(userId -> !userId.isEmpty())
                .distinct()
                .toList();
    }

    private static Pageable pageRequest(int page, int size) {
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private boolean isCollegeAdmin() {
        return hasAuthority("ROLE_" + RoleNames.COLLEGE_ADMIN);
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
