package com.example.accounts.service.impl;

import com.example.accounts.dto.CurrentUserContext;
import com.example.accounts.entity.AccessControlList;
import com.example.accounts.entity.Account;
import com.example.accounts.entity.Banker;
import com.example.accounts.entity.Customer;
import com.example.accounts.exception.AccessDeniedException;
import com.example.accounts.exception.ResourceNotFoundException;
import com.example.accounts.reository.AccessControlListRepository;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.reository.BankerRepository;
import com.example.accounts.reository.CustomerRepository;
import com.example.accounts.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for authorization and access control enforcement.
 *
 * Enforces:
 * 1. Ownership checks (CUSTOMER can only access own records)
 * 2. ACL checks (fine-grained whitelist permissions)
 * 3. BANKER scope (assigned accounts only)
 * 4. Role-based access control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {

    private final AccessControlListRepository aclRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final BankerRepository bankerRepository;

    @Override
    @Transactional(readOnly = true)
    public void validateAccountViewAccess(Long accountId, CurrentUserContext currentUser, String correlationId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // ADMIN and OPS can view all (OPS has read-only access)
        if (currentUser.isAdmin() || currentUser.isOps()) {
            log.debug("AccessControl: {} has {} role, full access granted. CorrelationId: {}",
                currentUser.getUserId(),
                currentUser.isAdmin() ? "ADMIN" : "OPS",
                correlationId);
            return;
        }

        // CUSTOMER: must own the account or have VIEW via ACL
        if (currentUser.isCustomer()) {
            // ================================================
            // STEP 1: Fetch customerId for this user
            // CustomerId is fetched from Customer table using userId
            // because customer record is created AFTER user registration
            // ================================================
            Long customerId = fetchCustomerIdByUserId(currentUser.getUserId());

            if (customerId == null) {
                log.warn("AccessControl: DENIED - No customer found for userId {}. CorrelationId: {}",
                    currentUser.getUserId(), correlationId);
                throw new AccessDeniedException(
                    "Access denied. You do not have a customer account.",
                    correlationId
                );
            }

            // ================================================
            // STEP 2: Check ownership
            // ================================================
            if (account.getCustomer().getCustomerId().equals(customerId)) {
                log.debug("AccessControl: Customer {} views own account {}. CorrelationId: {}",
                    currentUser.getUserId(), accountId, correlationId);
                return;
            }

            // ================================================
            // STEP 3: Else Check ACL for VIEW permission for non owning customer
            // ================================================
            if (hasAccountPermission(accountId, currentUser.getUserId(), AccessControlList.Permission.VIEW)) {
                log.debug("AccessControl: Customer {} has VIEW ACL on account {}. CorrelationId: {}",
                    currentUser.getUserId(), accountId, correlationId);
                return;
            }

            log.warn("AccessControl: DENIED - Customer {} cannot view account {} (not owner, no ACL). CorrelationId: {}",
                currentUser.getUserId(), accountId, correlationId);
            throw new AccessDeniedException(
                "Access denied. You cannot view this account.",
                correlationId
            );
        }

        // BANKER: only check if assigned to account (no ACL check)
        if (currentUser.isBanker()) {
            if (isBankerAssignedToAccount(currentUser.getUserId(), accountId)) {
                log.debug("AccessControl: Banker {} views assigned account {}. CorrelationId: {}",
                    currentUser.getUserId(), accountId, correlationId);
                return;
            }

            log.warn("AccessControl: DENIED - Banker {} not assigned to account {}. CorrelationId: {}",
                currentUser.getUserId(), accountId, correlationId);
            throw new AccessDeniedException(
                "Access denied. This account is not assigned to you.",
                correlationId
            );
        }

        log.warn("AccessControl: DENIED - Unknown role for user {}. CorrelationId: {}",
            currentUser.getUserId(), correlationId);
        throw new AccessDeniedException(
            "Access denied. Invalid role.",
            correlationId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAccountUpdateAccess(Long accountId, CurrentUserContext currentUser, String correlationId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // ADMIN has full access
        if (currentUser.isAdmin()) {
            log.debug("AccessControl: ADMIN {} can update account {}. CorrelationId: {}",
                currentUser.getUserId(), accountId, correlationId);
            return;
        }

        // OPS is read-only
        if (currentUser.isOps()) {
            log.warn("AccessControl: DENIED - OPS role has read-only access. CorrelationId: {}", correlationId);
            throw new AccessDeniedException(
                "Access denied. OPS role has read-only access.",
                correlationId
            );
        }

        if (currentUser.isCustomer()) {
            // ================================================
            // STEP 1: Fetch customerId for this user
            // CustomerId is fetched from Customer table using userId
            // because customer record is created AFTER user registration
            // ================================================
            Long customerId = fetchCustomerIdByUserId(currentUser.getUserId());

            if (customerId == null) {
                log.warn("AccessControl: DENIED - No customer found for userId {}. CorrelationId: {}",
                        currentUser.getUserId(), correlationId);
                throw new AccessDeniedException(
                        "Access denied. You do not have a customer account.",
                        correlationId
                );
            }

            // ================================================
            // STEP 2: Check ownership
            // ================================================
            if (account.getCustomer().getCustomerId().equals(customerId)) {
                log.debug("AccessControl: Customer {} views own account {}. CorrelationId: {}",
                        currentUser.getUserId(), accountId, correlationId);
                return;
            }

            // ================================================
            // STEP 3: Else Check ACL for VIEW permission for non owning customer
            // ================================================
            if (hasAccountPermission(accountId, currentUser.getUserId(), AccessControlList.Permission.VIEW)) {
                log.debug("AccessControl: Customer {} has VIEW ACL on account {}. CorrelationId: {}",
                        currentUser.getUserId(), accountId, correlationId);
                return;
            }

            log.warn("AccessControl: DENIED - Customer {} cannot view account {} (not owner, no ACL). CorrelationId: {}",
                    currentUser.getUserId(), accountId, correlationId);
            throw new AccessDeniedException(
                    "Access denied. You cannot view this account.",
                    correlationId
            );
        }

        // BANKER: only check if assigned to account (no ACL check)
        if (currentUser.isBanker()) {
            if (isBankerAssignedToAccount(currentUser.getUserId(), accountId)) {
                log.debug("AccessControl: Banker {} can update account {}. CorrelationId: {}",
                    currentUser.getUserId(), accountId, correlationId);
                return;
            }

            log.warn("AccessControl: DENIED - Banker {} not assigned to account {}. CorrelationId: {}",
                currentUser.getUserId(), accountId, correlationId);
            throw new AccessDeniedException(
                "Access denied. This account is not assigned to you.",
                correlationId
            );
        }


        log.warn("AccessControl: DENIED - Unknown role. CorrelationId: {}", correlationId);
        throw new AccessDeniedException(
            "Access denied. Invalid role.",
            correlationId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAccountDeleteAccess(Long accountId, CurrentUserContext currentUser, String correlationId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // ADMIN has full access
        if (currentUser.isAdmin()) {
            log.debug("AccessControl: ADMIN {} can delete account {}. CorrelationId: {}",
                currentUser.getUserId(), accountId, correlationId);
            return;
        }

        // OPS is read-only
        if (currentUser.isOps()) {
            log.warn("AccessControl: DENIED - OPS role has read-only access. CorrelationId: {}", correlationId);
            throw new AccessDeniedException(
                "Access denied. OPS role has read-only access.",
                correlationId
            );
        }

        // CUSTOMER: must own account AND have DELETE via ACL
        if (currentUser.isCustomer()) {
            // Fetch customerId from Customer table using userId
            Long customerId = fetchCustomerIdByUserId(currentUser.getUserId());

            if (customerId == null || !account.getCustomer().getCustomerId().equals(customerId)) {
                log.warn("AccessControl: DENIED - Customer {} does not own account {}. CorrelationId: {}",
                    currentUser.getUserId(), accountId, correlationId);
                throw new AccessDeniedException(
                    "Access denied. You cannot delete this account.",
                    correlationId
                );
            }

            if (!hasAccountPermission(accountId, currentUser.getUserId(), AccessControlList.Permission.DELETE)) {
                log.warn("AccessControl: DENIED - Customer {} lacks DELETE permission. CorrelationId: {}",
                    currentUser.getUserId(), correlationId);
                throw new AccessDeniedException(
                    "Access denied. You do not have DELETE permission on this account.",
                    correlationId
                );
            }

            log.debug("AccessControl: Customer {} can delete account {}. CorrelationId: {}",
                currentUser.getUserId(), accountId, correlationId);
            return;
        }

        // BANKER: only check if assigned to account (no ACL check)
        if (currentUser.isBanker()) {
            if (isBankerAssignedToAccount(currentUser.getUserId(), accountId)) {
                log.debug("AccessControl: Banker {} can delete account {}. CorrelationId: {}",
                    currentUser.getUserId(), accountId, correlationId);
                return;
            }

            log.warn("AccessControl: DENIED - Banker {} not assigned to account {}. CorrelationId: {}",
                currentUser.getUserId(), accountId, correlationId);
            throw new AccessDeniedException(
                "Access denied. This account is not assigned to you.",
                correlationId
            );
        }

        log.warn("AccessControl: DENIED - Unknown role. CorrelationId: {}", correlationId);
        throw new AccessDeniedException(
            "Access denied. Invalid role.",
            correlationId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void validateTransferSourceAccess(Long sourceAccountId, CurrentUserContext currentUser, String correlationId) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
            .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        // ADMIN has full access
        if (currentUser.isAdmin()) {
            log.debug("AccessControl: ADMIN {} can transfer from account {}. CorrelationId: {}",
                currentUser.getUserId(), sourceAccountId, correlationId);
            return;
        }

        // OPS cannot transfer
        if (currentUser.isOps()) {
            log.warn("AccessControl: DENIED - OPS cannot perform transfers. CorrelationId: {}", correlationId);
            throw new AccessDeniedException(
                "Access denied. OPS role cannot perform transfers.",
                correlationId
            );
        }

        // CUSTOMER: must own source account AND have TRANSFER permission
        if (currentUser.isCustomer()) {
            // Fetch customerId from Customer table using userId
            Long customerId = fetchCustomerIdByUserId(currentUser.getUserId());

            if (customerId == null || !sourceAccount.getCustomer().getCustomerId().equals(customerId)) {
                log.warn("AccessControl: DENIED - Customer {} does not own source account {}. CorrelationId: {}",
                    currentUser.getUserId(), sourceAccountId, correlationId);
                throw new AccessDeniedException(
                    "Access denied. You cannot transfer from this account.",
                    correlationId
                );
            }

            if (!hasAccountPermission(sourceAccountId, currentUser.getUserId(), AccessControlList.Permission.TRANSFER)) {
                log.warn("AccessControl: DENIED - Customer {} lacks TRANSFER permission on account {}. CorrelationId: {}",
                    currentUser.getUserId(), sourceAccountId, correlationId);
                throw new AccessDeniedException(
                    "Access denied. You do not have TRANSFER permission on this account.",
                    correlationId
                );
            }

            log.debug("AccessControl: Customer {} can transfer from account {}. CorrelationId: {}",
                currentUser.getUserId(), sourceAccountId, correlationId);
            return;
        }

        // BANKER: only check if assigned to account (no ACL check)
        if (currentUser.isBanker()) {
            if (isBankerAssignedToAccount(currentUser.getUserId(), sourceAccountId)) {
                log.debug("AccessControl: Banker {} can transfer from account {}. CorrelationId: {}",
                    currentUser.getUserId(), sourceAccountId, correlationId);
                return;
            }

            log.warn("AccessControl: DENIED - Banker {} not assigned to source account {}. CorrelationId: {}",
                currentUser.getUserId(), sourceAccountId, correlationId);
            throw new AccessDeniedException(
                "Access denied. This account is not assigned to you.",
                correlationId
            );
        }

        log.warn("AccessControl: DENIED - Unknown role. CorrelationId: {}", correlationId);
        throw new AccessDeniedException(
            "Access denied. Invalid role.",
            correlationId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void validateTransferDestinationAccess(Long destinationAccountId, CurrentUserContext currentUser, String correlationId) {
        Account destinationAccount = accountRepository.findById(destinationAccountId)
            .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        // ADMIN and BANKER can transfer to any account
        if (currentUser.isAdmin() || currentUser.isBanker()) {
            log.debug("AccessControl: {} can transfer to account {}. CorrelationId: {}",
                currentUser.isAdmin() ? "ADMIN" : "BANKER", destinationAccountId, correlationId);
            return;
        }

        // OPS cannot transfer
        if (currentUser.isOps()) {
            log.warn("AccessControl: DENIED - OPS cannot perform transfers. CorrelationId: {}", correlationId);
            throw new AccessDeniedException(
                "Access denied. OPS role cannot perform transfers.",
                correlationId
            );
        }

        // CUSTOMER: NO restriction (we already checked src account) (transfers allowed between different customers)
        if (currentUser.isCustomer()) {
            log.debug("AccessControl: Customer {} can transfer to account {}. CorrelationId: {}",
                currentUser.getUserId(), destinationAccountId, correlationId);
            return;
        }

        log.warn("AccessControl: DENIED - Unknown role. CorrelationId: {}", correlationId);
        throw new AccessDeniedException(
            "Access denied. Invalid role.",
            correlationId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCustomerAccess(Long customerId, CurrentUserContext currentUser, String correlationId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // ADMIN has full access
        if (currentUser.isAdmin()) {
            log.debug("AccessControl: ADMIN {} can access customer {}. CorrelationId: {}",
                currentUser.getUserId(), customerId, correlationId);
            return;
        }

        // OPS has read-only access
        if (currentUser.isOps()) {
            log.debug("AccessControl: OPS {} has read-only access to customer {}. CorrelationId: {}",
                currentUser.getUserId(), customerId, correlationId);
            return;
        }

        // CUSTOMER: can only access own customer record
        if (currentUser.isCustomer()) {
            // Fetch customerId from Customer table using userId
            Long userCustomerId = fetchCustomerIdByUserId(currentUser.getUserId());

            if (userCustomerId != null && userCustomerId.equals(customerId)) {
                log.debug("AccessControl: Customer {} accesses own record. CorrelationId: {}",
                    currentUser.getUserId(), correlationId);
                return;
            }

            log.warn("AccessControl: DENIED - Customer {} cannot access customer {}. CorrelationId: {}",
                currentUser.getUserId(), customerId, correlationId);
            throw new AccessDeniedException(
                "Access denied. You cannot access this customer record.",
                correlationId
            );
        }

        // BANKER: can only access assigned customers (no ACL check)
        if (currentUser.isBanker()) {
            if (isBankerAssignedToCustomer(currentUser.getUserId(), customerId)) {
                log.debug("AccessControl: Banker {} accesses assigned customer {}. CorrelationId: {}",
                    currentUser.getUserId(), customerId, correlationId);
                return;
            }

            log.warn("AccessControl: DENIED - Banker {} not assigned to customer {}. CorrelationId: {}",
                currentUser.getUserId(), customerId, correlationId);
            throw new AccessDeniedException(
                "Access denied. This customer is not assigned to you.",
                correlationId
            );
        }

        log.warn("AccessControl: DENIED - Unknown role. CorrelationId: {}", correlationId);
        throw new AccessDeniedException(
            "Access denied. Invalid role.",
            correlationId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccountPermission(Long accountId, Long userId, AccessControlList.Permission permission) {
        return aclRepository.findByAccountAccountIdAndUserId(accountId, userId)
            .map(acl -> acl.getPermission() == permission)
            .orElse(false);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isBankerAssignedToAccount(Long userId, Long accountId) {
        try {
            // Step 1: Find banker by userId
            Banker banker = bankerRepository.findByUserId(userId)
                .orElse(null);

            if (banker == null) {
                log.debug("No banker found for userId: {}", userId);
                return false;
            }

            // Step 2: Find account
            Account account = accountRepository.findById(accountId)
                .orElse(null);

            if (account == null) {
                log.debug("No account found with ID: {}", accountId);
                return false;
            }

            // Step 3: Check if banker is assigned to account
            if (account.getAssignedBanker() != null &&
                account.getAssignedBanker().getBankerId().equals(banker.getBankerId())) {
                log.debug("Banker {} is assigned to account {}", banker.getBankerId(), accountId);
                return true;
            }

            log.debug("Banker {} is NOT assigned to account {}", banker.getBankerId(), accountId);
            return false;

        } catch (Exception e) {
            log.warn("Error checking banker assignment to account {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Fetch customerId from Customer table using userId.
     *
     * This method is used to look up the customerId associated with a userId.
     * It's needed because:
     * - Customer record is created AFTER user registration
     * - CustomerId cannot be stored in JWT (not available at login time)
     * - We need to fetch it dynamically from Customer table when doing ACL checks
     *
     * @param userId The userId from JWT token
     * @return customerId associated with this userId, or null if not found
     */
    private Long fetchCustomerIdByUserId(Long userId) {
        try {
            if (userId == null) {
                log.debug("UserId is null, cannot fetch customerId");
                return null;
            }

            // Query Customer table: Find customer record with this userId
            // Customer.userId is a foreign key referencing User microservice
            Customer customer = customerRepository.findByUserId(userId)
                .orElse(null);

            if (customer == null) {
                log.debug("No customer found for userId: {}. User may not have created customer account yet.", userId);
                return null;
            }

            log.debug("Found customerId {} for userId {}", customer.getCustomerId(), userId);
            return customer.getCustomerId();

        } catch (Exception e) {
            log.warn("Failed to fetch customerId for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a banker is assigned to a customer.
     *
     * @param userId The userId of the banker (from JWT)
     * @param customerId The customer ID to check
     * @return true if the banker is assigned to this customer, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isBankerAssignedToCustomer(Long userId, Long customerId) {
        try {
            // Step 1: Find banker by userId
            Banker banker = bankerRepository.findByUserId(userId)
                .orElse(null);

            if (banker == null) {
                log.debug("No banker found for userId: {}", userId);
                return false;
            }

            // Step 2: Find customer
            Customer customer = customerRepository.findById(customerId)
                .orElse(null);

            if (customer == null) {
                log.debug("No customer found with ID: {}", customerId);
                return false;
            }

            // Step 3: Check if banker is assigned to customer
            if (customer.getAssignedBanker() != null &&
                customer.getAssignedBanker().getBankerId().equals(banker.getBankerId())) {
                log.debug("Banker {} is assigned to customer {}", banker.getBankerId(), customerId);
                return true;
            }

            log.debug("Banker {} is NOT assigned to customer {}", banker.getBankerId(), customerId);
            return false;

        } catch (Exception e) {
            log.warn("Error checking banker assignment to customer {}: {}", customerId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAdminAccess(CurrentUserContext currentUser, String correlationId) {
        // Check if user has ADMIN role
        if (!currentUser.isAdmin()) {
            log.warn("AccessControl: DENIED - User {} attempted admin operation without ADMIN role. CorrelationId: {}",
                currentUser.getUserId(), correlationId);
            throw new AccessDeniedException(
                "Access denied. Only ADMIN can perform this operation.",
                correlationId
            );
        }

        log.debug("AccessControl: ADMIN {} has access to admin operations. CorrelationId: {}",
            currentUser.getUserId(), correlationId);
    }
}


