package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_actor_id", columnList = "actor_id"),
    @Index(name = "idx_entity_type", columnList = "entity_type"),
    @Index(name = "idx_entity_id", columnList = "entity_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "request_context", columnDefinition = "TEXT")
    private String requestContext;

    @Column(name = "redacted_payload", columnDefinition = "TEXT")
    private String redactedPayload;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AuditStatus status;

    @Column(name = "ip_address")
    private String ipAddress;

    public enum AuditAction {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        TRANSFER_REQUEST,
        TRANSFER_AUTHORIZE,
        TRANSFER_POST,
        TRANSFER_CANCEL,
        ACCOUNT_FREEZE,
        ACCOUNT_UNFREEZE,
        ACCOUNT_CLOSE
    }

    public enum AuditStatus {
        SUCCESS,
        FAILURE,
        ACCESS_DENIED
    }
}

