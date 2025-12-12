package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "idempotency_keys", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class IdempotencyKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Long expiresAt;
}

