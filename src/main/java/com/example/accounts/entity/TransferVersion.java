package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transfer_versions", indexes = {
    @Index(name = "idx_transfer_id", columnList = "transfer_id"),
    @Index(name = "idx_version_number", columnList = "version_number")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class TransferVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    @ToString.Exclude
    private Transfer transfer;

    @Column(name = "version_number", nullable = false)
    private Long versionNumber;

    @Column(name = "source_account_id", nullable = false)
    private Long sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private Long destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;
}

