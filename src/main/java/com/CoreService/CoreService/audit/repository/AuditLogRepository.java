package com.CoreService.CoreService.audit.repository;

import com.CoreService.CoreService.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Filters are optional; the college is not. Passing null for a filter drops
     * that predicate, so one query serves every combination the read API offers.
     */
    @Query("""
            select l
              from AuditLog l
             where l.collegeId = :collegeId
               and (:action is null or l.action = :action)
               and (:resourceType is null or l.resourceType = :resourceType)
               and (:userId is null or l.userId = :userId)
               and (:from is null or l.createdAt >= :from)
               and (:to is null or l.createdAt <= :to)
            """)
    Page<AuditLog> search(@Param("collegeId") UUID collegeId,
                          @Param("action") String action,
                          @Param("resourceType") String resourceType,
                          @Param("userId") String userId,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
