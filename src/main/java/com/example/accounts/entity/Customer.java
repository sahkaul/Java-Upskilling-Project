package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_kyc_id", columnList = "kyc_id")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;  // Reference to User microservice

    /**
     * Banker assigned to manage this customer
     * Optional - customer may not be assigned to any banker
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banker_id")
    @ToString.Exclude
    private Banker assignedBanker;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "kyc_id", unique = true)
    private String kycId;

    @Column(name = "encryption_version", nullable = false)
    private Integer encryptionVersion = 1;

    @Column(name = "last_encrypted_on")
    private Long lastEncryptedOn;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<AccessControlList> accessControls = new ArrayList<>();
}
