package com.example.accounts.service;

import com.example.accounts.dto.BankerDto;
import com.example.accounts.dto.AccountsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


/**
 * Service interface for Banker management
 *
 * Responsibilities:
 * - CRUD operations on banker records
 * - Assign/unassign customers and accounts to bankers
 * - Manage banker portfolio and assignments
 */
public interface BankerService {

    /**
     * Create a new banker
     */
    BankerDto createBanker(BankerDto bankerDto);

    /**
     * Get banker by ID
     */
    BankerDto getBankerById(Long bankerId);

    /**
     * Get banker by userId
     */
    BankerDto getBankerByUserId(Long userId);


    /**
     * Update banker information
     */
    BankerDto updateBanker(Long bankerId, BankerDto bankerDto);

    /**
     * Deactivate banker (soft delete)
     */
    void deactivateBanker(Long bankerId);

    /**
     * Assign customer to banker
     */
    void assignCustomerToBanker(Long bankerId, Long customerId, String correlationId);

    /**
     * Unassign customer from banker
     *
     * @param bankerId the banker ID to unassign from (must exist)
     * @param customerId the customer ID to unassign
     * @param correlationId for audit logging
     */
    void unassignCustomerFromBanker(Long bankerId, Long customerId, String correlationId);

    /**
     * Assign account to banker
     */
    void assignAccountToBanker(Long bankerId, Long accountId, String correlationId);

    /**
     * Unassign account from banker
     *
     * @param bankerId the banker ID to unassign from (must exist)
     * @param accountId the account ID to unassign
     * @param correlationId for audit logging
     */
    void unassignAccountFromBanker(Long bankerId, Long accountId, String correlationId);

    /**
     * Get customers assigned to banker
     */
    Page<Long> getAssignedCustomers(Long bankerId, Pageable pageable);

    /**
     * Get accounts assigned to banker
     */
    Page<Long> getAssignedAccounts(Long bankerId, Pageable pageable);


    /**
     * Create account for assigned customer
     *
     * @param customerId the customer ID to create account for
     * @param accountsDto the account details (type, currency, branch)
     * @param correlationId for audit logging
     * @return the created account details
     */
    AccountsDto createAccountForCustomer(Long customerId, AccountsDto accountsDto, String correlationId);
}

