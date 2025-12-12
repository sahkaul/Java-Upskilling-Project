package com.example.accounts.controller;

import com.example.accounts.dto.AuditLogDto;
import com.example.accounts.dto.ApiResponse;
import com.example.accounts.service.AuditService;
import com.example.accounts.util.GeneratorUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Management", description = "APIs for accessing audit logs")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Get all audit logs")
    public ResponseEntity<ApiResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogDto> logs = auditService.getAuditLogs(pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Audit logs retrieved successfully",
            correlationId,
            logs,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/actor/{actorId}")
    @Operation(summary = "Get audit logs by actor")
    public ResponseEntity<ApiResponse> getAuditLogsByActor(
            @PathVariable Long actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogDto> logs = auditService.getAuditLogsByActor(actorId, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Audit logs retrieved successfully",
            correlationId,
            logs,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs by entity")
    public ResponseEntity<ApiResponse> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogDto> logs = auditService.getAuditLogsByEntity(entityType, entityId, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Audit logs retrieved successfully",
            correlationId,
            logs,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get audit logs by date range")
    public ResponseEntity<ApiResponse> getAuditLogsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogDto> logs = auditService.getAuditLogsByDateRange(start, end, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Audit logs retrieved successfully",
            correlationId,
            logs,
            null
        );
        return ResponseEntity.ok(response);
    }
}

