package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_number", columnList = "account_number", unique = true),
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_account_status", columnList = "account_status")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    /**
     * Banker assigned to manage this account
     * Optional - account may not be assigned to any banker
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banker_id")
    @ToString.Exclude
    private Banker assignedBanker;

    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;  // SAVINGS, CURRENT

    @Column(name = "account_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false)
    private String currency = "USD";

    @Column(name = "branch_address")
    private String branchAddress;

    @Column(name = "frozen_reason", columnDefinition = "TEXT")
    private String frozenReason;

    @Column(name = "frozen_on")
    private LocalDateTime frozenOn;

    @Column(name = "closed_reason", columnDefinition = "TEXT")
    private String closedReason;

    @Column(name = "closed_on")
    private LocalDateTime closedOn;

    @Column(name = "encryption_version", nullable = false)
    private Integer encryptionVersion = 1;

    @Column(name = "last_encrypted_on")
    private Long lastEncryptedOn;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    @OneToMany(mappedBy = "sourceAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Transfer> sourceTransfers = new ArrayList<>();

    @OneToMany(mappedBy = "destinationAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Transfer> destinationTransfers = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<TransferHold> transferHolds = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<AccessControlList> accessControls = new ArrayList<>();

    public enum AccountType {
        SAVINGS,
        CURRENT
    }

    public enum AccountStatus {
        ACTIVE,
        FROZEN,
        CLOSED
    }
}
