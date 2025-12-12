package com.example.accounts.service;

import com.example.accounts.dto.CurrentUserContext;
import com.example.accounts.entity.AccessControlList;

/**
 * Service interface for authorization and access control enforcement.
 *
 * Implements the following rules:
 * 1. CUSTOMER can only access their own customer/account records
 * 2. BANKER can only access accounts assigned to them
 * 3. ACL can grant VIEW/UPDATE/DELETE to specific users for accounts
 * 4. All denials return 403 with correlationId for auditing
 */
public interface AuthorizationService {

    /**
     * Verify that current user can access (view) the specified account.
     * <p>
     * Rules:
     * - CUSTOMER: owns the account OR has VIEW permission via ACL
     * - BANKER: account is assigned to them OR has VIEW permission via ACL
     * - OPS: read-only access allowed
     * - ADMIN: full access
     *
     * @param accountId     the account ID to check
     * @param currentUser   the current authenticated user
     * @param correlationId for auditing access denials
     *                      ""
     */
    void validateAccountViewAccess(Long accountId, CurrentUserContext currentUser, String correlationId);

    /**
     * Verify that current user can update (modify) the specified account.
     * <p>
     * Rules:
     * - CUSTOMER: owns the account AND has UPDATE permission via ACL (or is the only owner)
     * - BANKER: account is assigned to them AND has UPDATE permission via ACL
     * - OPS: no write access
     * - ADMIN: full access
     *
     * @param accountId     the account ID to check
     * @param currentUser   the current authenticated user
     * @param correlationId for auditing access denials
     *                      ""
     */
    void validateAccountUpdateAccess(Long accountId, CurrentUserContext currentUser, String correlationId);

    /**
     * Verify that current user can delete the specified account.
     * <p>
     * Rules:
     * - CUSTOMER: owns the account AND has DELETE permission via ACL
     * - BANKER: account is assigned to them AND has DELETE permission via ACL
     * - OPS: no write access
     * - ADMIN: full access
     *
     * @param accountId     the account ID to check
     * @param currentUser   the current authenticated user
     * @param correlationId for auditing access denials
     *                      ""
     */
    void validateAccountDeleteAccess(Long accountId, CurrentUserContext currentUser, String correlationId);

    /**
     * Verify that current user can perform a transfer FROM the source account.
     * <p>
     * Rules:
     * - CUSTOMER: owns the source account AND has TRANSFER permission via ACL
     * - BANKER: source account is assigned to them AND has TRANSFER permission
     * - OPS: no write access
     * - ADMIN: full access
     *
     * @param sourceAccountId the source account ID
     * @param currentUser     the current authenticated user
     * @param correlationId   for auditing access denials
     *                        ""
     */
    void validateTransferSourceAccess(Long sourceAccountId, CurrentUserContext currentUser, String correlationId);

    /**
     * Verify that current user can perform a transfer TO the destination account.
     * <p>
     * Rules:
     * - CUSTOMER: no restrictions (can transfer to any account (same as full access over here)
     * - BANKER/ADMIN: full access AND OPS : no access
     *
     * @param destinationAccountId the destination account ID
     * @param currentUser          the current authenticated user
     * @param correlationId        for auditing access denials
     *                             ""
     */
    void validateTransferDestinationAccess(Long destinationAccountId, CurrentUserContext currentUser, String correlationId);

    /**
     * Verify that current user can access the specified customer record.
     * <p>
     * Rules:
     * - CUSTOMER: can only access own customer record
     * - BANKER: can access assigned customers
     * - OPS/ADMIN: read-only access
     *
     * @param customerId    the customer ID to check
     * @param currentUser   the current authenticated user
     * @param correlationId for auditing access denials
     *                      ""
     */
    //Can add this when getting customerData?
    void validateCustomerAccess(Long customerId, CurrentUserContext currentUser, String correlationId);

    /**
     * Check if current user has permission on account via ACL.
     *
     * @param accountId  the account ID
     * @param userId     the user ID
     * @param permission the required permission (VIEW, UPDATE, DELETE, TRANSFER)
     * @return true if user has the permission, false otherwise
     */
    boolean hasAccountPermission(Long accountId, Long userId, AccessControlList.Permission permission);


    /**
     * Check if current user (banker) has the account assigned.
     *
     * @param accountId the account ID
     * @param userId    the user ID (banker)
     * @return true if account is assigned to this banker, false otherwise
     */
    boolean isBankerAssignedToAccount(Long accountId, Long userId);

    /**
     * Verify that current user is an ADMIN.
     * Used for admin-only operations like assigning/unassigning customers and accounts to bankers.
     *
     * @param currentUser   the current authenticated user
     * @param correlationId for auditing access denials
     */
    void validateAdminAccess(CurrentUserContext currentUser, String correlationId);
}
