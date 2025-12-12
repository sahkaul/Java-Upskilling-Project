package com.example.accounts.service.impl;

import com.example.accounts.dto.AuditLogDto;
import com.example.accounts.entity.AuditLog;
import com.example.accounts.reository.AuditLogRepository;
import com.example.accounts.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logAction(Long actorId, String action, String entityType, Long entityId, String correlationId, String status) {
        logActionWithContext(actorId, action, entityType, entityId, correlationId, null, null, status);
    }

    @Override
    @Transactional
    public void logActionWithContext(Long actorId, String action, String entityType, Long entityId, String correlationId,
                                     String requestContext, String redactedPayload, String status) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(actorId);
        auditLog.setAction(AuditLog.AuditAction.valueOf(action));
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setCorrelationId(correlationId);
        auditLog.setRequestContext(requestContext);
        auditLog.setRedactedPayload(redactedPayload);
        auditLog.setStatus(AuditLog.AuditStatus.valueOf(status));

        auditLogRepository.save(auditLog);
        log.debug("Audit log created. Action: {}, Entity: {}, Correlation ID: {}", action, entityType, correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
            .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByActor(Long actorId, Pageable pageable) {
        return auditLogRepository.findByActorId(actorId, pageable)
            .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityId(entityId, pageable)
            .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findAuditsByDateRange(startDate, endDate, pageable)
            .map(this::convertToDto);
    }

    private AuditLogDto convertToDto(AuditLog auditLog) {
        return new AuditLogDto(
            auditLog.getAuditId(),
            auditLog.getActorId(),
            auditLog.getAction().toString(),
            auditLog.getEntityType(),
            auditLog.getEntityId(),
            auditLog.getCorrelationId(),
            auditLog.getStatus().toString(),
            auditLog.getCreatedAt(),
            auditLog.getIpAddress(),
            auditLog.getRequestContext()
        );
    }
}

