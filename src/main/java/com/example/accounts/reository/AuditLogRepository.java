package com.example.accounts.reository;

import com.example.accounts.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT al FROM AuditLog al WHERE al.actorId = :actorId ORDER BY al.createdAt DESC")
    Page<AuditLog> findByActorId(@Param("actorId") Long actorId, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.action = :action ORDER BY al.createdAt DESC")
    Page<AuditLog> findByAction(@Param("action") AuditLog.AuditAction action, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType ORDER BY al.createdAt DESC")
    Page<AuditLog> findByEntityType(@Param("entityType") String entityType, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.createdAt >= :startDate AND al.createdAt <= :endDate ORDER BY al.createdAt DESC")
    Page<AuditLog> findAuditsByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.entityId = :entityId ORDER BY al.createdAt DESC")
    Page<AuditLog> findByEntityId(@Param("entityId") Long entityId, Pageable pageable);
}

