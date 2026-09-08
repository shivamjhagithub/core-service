package com.CoreService.CoreService.announcement.service;

import com.CoreService.CoreService.announcement.dto.AnnouncementRequest;
import com.CoreService.CoreService.announcement.dto.AnnouncementResponse;
import com.CoreService.CoreService.announcement.entity.Announcement;
import com.CoreService.CoreService.announcement.enums.AnnouncementTarget;
import com.CoreService.CoreService.announcement.event.AnnouncementPublishedEvent;
import com.CoreService.CoreService.announcement.mapper.AnnouncementMapper;
import com.CoreService.CoreService.announcement.repository.AnnouncementRepository;
import com.CoreService.CoreService.audit.enums.AuditAction;
import com.CoreService.CoreService.audit.service.AuditRecorder;
import com.CoreService.CoreService.classroom.service.ClassroomAccessService;
import com.CoreService.CoreService.common.exception.BusinessException;
import com.CoreService.CoreService.common.exception.ForbiddenException;
import com.CoreService.CoreService.common.exception.ResourceNotFoundException;
import com.CoreService.CoreService.common.response.PageResponse;
import com.CoreService.CoreService.common.security.ModuleCodes;
import com.CoreService.CoreService.common.security.RoleNames;
import com.CoreService.CoreService.common.security.TenantGuard;
import com.CoreService.CoreService.module.Services.ModuleAccessGuard;
import com.CoreService.CoreService.user.Entities.UserEntity;
import com.CoreService.CoreService.user.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final String RESOURCE_TYPE = "Announcement";

    /** Targets whose audience is the whole college. */
    private static final Set<AnnouncementTarget> COLLEGE_WIDE_TARGETS =
            Set.of(AnnouncementTarget.COLLEGE, AnnouncementTarget.DEPARTMENT);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;
    private final ClassroomAccessService classroomAccessService;
    private final UserRepo userRepo;
    private final TenantGuard tenantGuard;
    private final ModuleAccessGuard moduleAccessGuard;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditRecorder auditRecorder;

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ANNOUNCEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();

        validateTarget(request, collegeId);
        assertMayAddress(request.target(), request.targetId());

        Announcement announcement = new Announcement();
        announcement.setCollegeId(collegeId);
        announcement.setTitle(request.title().trim());
        announcement.setBody(request.body().trim());
        announcement.setTarget(request.target());
        announcement.setTargetId(request.targetId());
        announcement.setTargetUserId(normalize(request.targetUserId()));
        announcement.setCreatedBy(userId);
        announcement.setPublished(false);

        Announcement saved = announcementRepository.save(announcement);
        auditRecorder.record(AuditAction.ANNOUNCEMENT_CREATED, RESOURCE_TYPE, saved.getId());

        if (request.publishNow()) {
            publishInternal(saved);
        }

        return announcementMapper.toResponse(saved);
    }

    @Transactional
    public AnnouncementResponse publish(UUID announcementId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ANNOUNCEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();

        Announcement announcement = announcementRepository.findByIdAndCollegeId(announcementId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, announcementId));

        if (announcement.isPublished()) {
            throw new BusinessException("This announcement has already been published");
        }

        assertMayAddress(announcement.getTarget(), announcement.getTargetId());
        publishInternal(announcement);

        return announcementMapper.toResponse(announcement);
    }

    @Transactional
    public void delete(UUID announcementId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ANNOUNCEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();

        Announcement announcement = announcementRepository.findByIdAndCollegeId(announcementId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, announcementId));

        if (!userId.equals(announcement.getCreatedBy()) && !hasRole(RoleNames.COLLEGE_ADMIN)) {
            throw new ForbiddenException("Only the author or a college administrator may delete an announcement");
        }

        announcementRepository.delete(announcement);
        auditRecorder.record(AuditAction.ANNOUNCEMENT_DELETED, RESOURCE_TYPE, announcementId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> visibleToMe(Pageable pageable) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ANNOUNCEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();

        List<UUID> classroomIds = classroomAccessService.classroomIdsOfUser(userId);

        Page<Announcement> page = (classroomIds == null || classroomIds.isEmpty())
                ? announcementRepository.findVisibleWithoutClassrooms(collegeId, userId,
                COLLEGE_WIDE_TARGETS, AnnouncementTarget.USER, pageable)
                : announcementRepository.findVisible(collegeId, userId,
                COLLEGE_WIDE_TARGETS, AnnouncementTarget.USER, AnnouncementTarget.CLASSROOM,
                classroomIds, pageable);

        return PageResponse.from(page, announcementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getById(UUID announcementId) {
        moduleAccessGuard.assertModuleEnabled(ModuleCodes.ANNOUNCEMENT);
        UUID collegeId = tenantGuard.requireCollegeId();
        String userId = tenantGuard.requireUserId();

        Announcement announcement = announcementRepository.findByIdAndCollegeId(announcementId, collegeId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_TYPE, announcementId));

        if (!isVisibleTo(announcement, userId)) {
            throw new ForbiddenException("This announcement is not addressed to you");
        }

        return announcementMapper.toResponse(announcement);
    }

    private void publishInternal(Announcement announcement) {
        List<String> recipients = resolveRecipients(announcement);

        announcement.setPublished(true);
        announcement.setPublishedAt(LocalDateTime.now());

        auditRecorder.record(AuditAction.ANNOUNCEMENT_PUBLISHED, RESOURCE_TYPE, announcement.getId());

        // Consumed by the notification module after this transaction commits.
        eventPublisher.publishEvent(new AnnouncementPublishedEvent(
                announcement.getCollegeId(),
                announcement.getId(),
                announcement.getTitle(),
                recipients,
                Instant.now()));
    }

    private List<String> resolveRecipients(Announcement announcement) {
        return switch (announcement.getTarget()) {
            // The academic module publishes no department membership lookup, so a
            // department announcement currently reaches the whole college. The
            // department id is stored, so narrowing the audience later needs no
            // data change - and addressing a department is restricted to college
            // administrators for exactly that reason.
            case COLLEGE, DEPARTMENT -> userRepo.findAllByCollegeId(announcement.getCollegeId()).stream()
                    .map(UserEntity::getUserId)
                    .toList();
            case CLASSROOM -> classroomAccessService.memberUserIds(announcement.getTargetId());
            case USER -> List.of(announcement.getTargetUserId());
        };
    }

    private void validateTarget(AnnouncementRequest request, UUID collegeId) {
        switch (request.target()) {
            case COLLEGE -> {
                if (request.targetId() != null || normalize(request.targetUserId()) != null) {
                    throw new BusinessException("A college announcement must not carry a target");
                }
            }
            case DEPARTMENT -> {
                if (request.targetId() == null) {
                    throw new BusinessException("A department announcement requires the department id in targetId");
                }
                if (normalize(request.targetUserId()) != null) {
                    throw new BusinessException("A department announcement must not carry a target user");
                }
            }
            case CLASSROOM -> {
                if (request.targetId() == null) {
                    throw new BusinessException("A classroom announcement requires the classroom id in targetId");
                }
                if (normalize(request.targetUserId()) != null) {
                    throw new BusinessException("A classroom announcement must not carry a target user");
                }
            }
            case USER -> {
                String targetUserId = normalize(request.targetUserId());
                if (targetUserId == null) {
                    throw new BusinessException("A direct announcement requires targetUserId");
                }
                if (request.targetId() != null) {
                    throw new BusinessException("A direct announcement must not carry a target id");
                }
                if (!userRepo.existsByCollegeIdAndUserId(collegeId, targetUserId)) {
                    throw new ResourceNotFoundException("User", targetUserId);
                }
            }
        }
    }

    private void assertMayAddress(AnnouncementTarget target, UUID targetId) {
        switch (target) {
            case COLLEGE, DEPARTMENT -> {
                if (!hasRole(RoleNames.COLLEGE_ADMIN)) {
                    throw new ForbiddenException(
                            "Only a college administrator may address the whole college");
                }
            }
            case CLASSROOM -> classroomAccessService.assertTeacher(targetId);
            case USER -> {
                // Anyone allowed to create announcements may address one person.
            }
        }
    }

    private boolean isVisibleTo(Announcement announcement, String userId) {
        if (userId.equals(announcement.getCreatedBy()) || hasRole(RoleNames.COLLEGE_ADMIN)) {
            return true;
        }
        if (!announcement.isPublished()) {
            return false;
        }
        return switch (announcement.getTarget()) {
            case COLLEGE, DEPARTMENT -> true;
            case CLASSROOM -> classroomAccessService.isMember(announcement.getTargetId(), userId);
            case USER -> userId.equals(announcement.getTargetUserId());
        };
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
