package com.example.accounts.service;

import com.example.accounts.dto.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditService {
    void logAction(Long actorId, String action, String entityType, Long entityId, String correlationId, String status);

    void logActionWithContext(Long actorId, String action, String entityType, Long entityId, String correlationId, String requestContext, String redactedPayload, String status);

    Page<AuditLogDto> getAuditLogs(Pageable pageable);

    Page<AuditLogDto> getAuditLogsByActor(Long actorId, Pageable pageable);

    Page<AuditLogDto> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable);

    Page<AuditLogDto> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}

