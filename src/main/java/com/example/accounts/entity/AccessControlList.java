package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "access_control_lists", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_customer_id", columnList = "customer_id")
})
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class AccessControlList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acl_id")
    private Long aclId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @ToString.Exclude
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @ToString.Exclude
    private Customer customer;

    @Column(name = "user_id", nullable = false)
    private Long userId;  // Reference to User microservice

    @Column(name = "permission", nullable = false)
    @Enumerated(EnumType.STRING)
    private Permission permission;

    public enum Permission {
        VIEW,
        UPDATE,
        DELETE,
        TRANSFER
    }
}

