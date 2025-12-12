package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_txn_id", columnList = "ledger_txn_id"),
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class LedgerEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_entry_id")
    private Long ledgerEntryId;

    @Column(name = "ledger_txn_id", nullable = false, length = 50)
    private String ledgerTxnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @ToString.Exclude
    private Account account;

    @Column(name = "entry_side", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntryType entryType;  // DEBIT, CREDIT

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reference_type")
    private String referenceType;  // TRANSFER, INTEREST, FEE

    @Column(name = "reference_id")
    private Long referenceId;

    public enum EntryType {
        DEBIT,
        CREDIT
    }
}

