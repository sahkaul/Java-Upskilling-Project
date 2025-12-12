package com.example.accounts.service;

import com.example.accounts.dto.AclRequestDto;
import com.example.accounts.dto.AclResponseDto;
import com.example.accounts.entity.AccessControlList;

import java.util.List;

/**
 * Service interface for managing Access Control Lists (ACL).
 * Handles granting, updating, and removing permissions for users on accounts.
 */
public interface AclService {

    /**
     * Add a new ACL entry (grant permission to a user for an account).
     *
     * @param request ACL request with accountId, userId, and permission
     * @param correlationId for tracking
     * @return AclResponseDto with the created ACL entry
     */
    AclResponseDto addAcl(AclRequestDto request, String correlationId);

    /**
     * Update an existing ACL entry (change user's permission for an account).
     *
     * @param aclId the ACL entry ID
     * @param request ACL request with updated permission
     * @param correlationId for tracking
     * @return AclResponseDto with the updated ACL entry
     */
    AclResponseDto updateAcl(Long aclId, AclRequestDto request, String correlationId);

    /**
     * Delete an ACL entry (revoke permission from a user for an account).
     *
     * @param aclId the ACL entry ID
     * @param correlationId for tracking
     */
    void deleteAcl(Long aclId, String correlationId);

    /**
     * Get all ACL entries for a specific account.
     *
     * @param accountId the account ID
     * @return List of AclResponseDto for that account
     */
    List<AclResponseDto> getAclsByAccount(Long accountId);

    /**
     * Get all ACL entries for a specific user and account.
     *
     * @param userId the user ID
     * @param accountId the account ID
     * @return AclResponseDto if exists, empty if not
     */
    AclResponseDto getAclByUserAndAccount(Long userId, Long accountId);

    /**
     * Check if a user has a specific permission on an account.
     *
     * @param userId the user ID
     * @param accountId the account ID
     * @param permission the permission to check
     * @return true if user has that permission
     */
    boolean hasPermission(Long userId, Long accountId, AccessControlList.Permission permission);
}

