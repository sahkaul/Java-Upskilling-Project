package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AuditLog", description = "Schema for audit log")
public class AuditLogDto {

    private Long auditId;

    private Long actorId;

    private String action;

    private String entityType;

    private Long entityId;

    private String correlationId;

    private String status;

    private LocalDateTime createdAt;

    private String ipAddress;

    private String requestContext;
}

