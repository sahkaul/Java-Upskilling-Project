package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transfer_holds", indexes = {
    @Index(name = "idx_transfer_id", columnList = "transfer_id"),
    @Index(name = "idx_account_id", columnList = "account_id")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class TransferHold extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hold_id")
    private Long holdId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    @ToString.Exclude
    private Transfer transfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @ToString.Exclude
    private Account account;

    @Column(name = "hold_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal holdAmount;

    @Column(name = "released", nullable = false)
    private Boolean released = false;

    @Column(name = "released_on")
    private Long releasedOn;
}

