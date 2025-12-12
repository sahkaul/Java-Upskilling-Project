package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transfers", indexes = {
    @Index(name = "idx_transfer_id", columnList = "transfer_id"),
    @Index(name = "idx_source_account_id", columnList = "source_account_id"),
    @Index(name = "idx_destination_account_id", columnList = "destination_account_id"),
    @Index(name = "idx_transfer_status", columnList = "transfer_status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class Transfer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    @ToString.Exclude
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id", nullable = false)
    @ToString.Exclude
    private Account destinationAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "transfer_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferStatus transferStatus = TransferStatus.REQUESTED;

    @Column(name = "initiated_by", nullable = false)
    private Long initiatedBy;  // userId from User microservice

    @Column(name = "authorized_by")
    private Long authorizedBy;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "ledger_txn_id")
    private String ledgerTxnId;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "version", nullable = true)
    private Long currentVersion = 1L;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<TransferVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<TransferHold> holds = new ArrayList<>();

    public enum TransferStatus {
        REQUESTED,
        AUTHORIZED,
        POSTED,
        CANCELLED,
        REJECTED
    }
}

