package com.example.accounts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a BANKER user.
 *
 * A BANKER can:
 * - View/manage customers and accounts assigned to them
 * - Perform administrative CRUD on assigned customers/accounts
 * - Create accounts, freeze/unfreeze, close accounts with audit
 * - Initiate operational transfers for assigned accounts
 */
@Entity
@Table(name = "bankers", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_branch_code", columnList = "branch_code"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
@Getter
@Setter
@ToString(exclude = {"assignedAccounts", "assignedCustomers"})
@AllArgsConstructor
@NoArgsConstructor
public class Banker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banker_id")
    private Long bankerId;

    /**
     * Foreign key to User microservice
     * Links to the user who has BANKER role
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * Branch code/code where banker operates
     * Used for scoping banker's access to customers/accounts in that branch
     */
    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    /**
     * Portfolio/Department name
     */
    @Column(name = "portfolio", length = 100)
    private String portfolio;

    /**
     * Is this banker account active?
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * One-to-Many relationship: A banker can manage multiple accounts
     * An account can be assigned to only one banker (or none)
     */
    @OneToMany(mappedBy = "assignedBanker", cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    private List<Account> assignedAccounts = new ArrayList<>();

    /**
     * One-to-Many relationship: A banker can manage multiple customers
     * A customer can be assigned to only one banker (or none)
     */
    @OneToMany(mappedBy = "assignedBanker", cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    private List<Customer> assignedCustomers = new ArrayList<>();
}

