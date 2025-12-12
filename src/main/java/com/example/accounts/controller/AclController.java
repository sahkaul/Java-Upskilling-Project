package com.example.accounts.controller;

import com.example.accounts.dto.AclRequestDto;
import com.example.accounts.dto.AclResponseDto;
import com.example.accounts.dto.ApiResponse;
import com.example.accounts.dto.CurrentUserContext;
import com.example.accounts.service.AclService;
import com.example.accounts.service.AuthorizationService;
import com.example.accounts.util.GeneratorUtil;
import com.example.accounts.util.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing Access Control Lists (ACL).
 * Handles granting, updating, and revoking permissions for users on accounts.
 *
 * ACL allows granular control over who can VIEW, UPDATE, DELETE, or TRANSFER on an account.
 * Only ADMIN can manage ACLs.
 */
@RestController
@RequestMapping("/api/acls")
@RequiredArgsConstructor
@Tag(name = "Access Control List (ACL) Management", description = "APIs for managing ACL entries")
public class AclController {

    private final AclService aclService;
    private final AuthorizationService authorizationService;

    /**
     * Add a new ACL entry (grant permission to a user for an account).
     *
     * Only ADMIN can create ACL entries.
     *
     * Request:
     * {
     *   "accountId": 1,
     *   "userId": 5,
     *   "permission": "VIEW"
     * }
     *
     * Response: 201 Created with AclResponseDto
     *
     * @param request AclRequestDto with accountId, userId, and permission
     * @return ApiResponse with created ACL entry
     */
    @PostMapping
    @Operation(summary = "Add a new ACL entry")
    public ResponseEntity<ApiResponse> addAcl(@Valid @RequestBody AclRequestDto request) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        //  Extract current user
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // Only ADMIN can create ACL entries
        if (!currentUser.isAdmin()) {
            throw new com.example.accounts.exception.AccessDeniedException(
                "Only ADMIN can create ACL entries",
                correlationId
            );
        }

        AclResponseDto acl = aclService.addAcl(request, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "ACL entry created successfully",
            correlationId,
            acl,
            null
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update an existing ACL entry (change user's permission for an account).
     *
     * Only ADMIN can update ACL entries.
     * Cannot change accountId or userId - only permission can be changed.
     *
     * Request:
     * {
     *   "accountId": 1,
     *   "userId": 5,
     *   "permission": "UPDATE"
     * }
     *
     * Response: 200 OK with updated AclResponseDto
     *
     * @param aclId the ACL entry ID
     * @param request AclRequestDto with updated permission
     * @return ApiResponse with updated ACL entry
     */
    @PutMapping("/{aclId}")
    @Operation(summary = "Update an ACL entry")
    public ResponseEntity<ApiResponse> updateAcl(
            @PathVariable Long aclId,
            @Valid @RequestBody AclRequestDto request) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        //  Extract current user
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // Only ADMIN can update ACL entries
        if (!currentUser.isAdmin()) {
            throw new com.example.accounts.exception.AccessDeniedException(
                "Only ADMIN can update ACL entries",
                correlationId
            );
        }

        AclResponseDto acl = aclService.updateAcl(aclId, request, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "ACL entry updated successfully",
            correlationId,
            acl,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an ACL entry (revoke permission from a user for an account).
     *
     * Only ADMIN can delete ACL entries.
     *
     * Response: 200 OK with success message
     *
     * @param aclId the ACL entry ID
     * @return ApiResponse with success message
     */
    @DeleteMapping("/{aclId}")
    @Operation(summary = "Delete an ACL entry")
    public ResponseEntity<ApiResponse> deleteAcl(@PathVariable Long aclId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        //  Extract current user
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // Only ADMIN can delete ACL entries
        if (!currentUser.isAdmin()) {
            throw new com.example.accounts.exception.AccessDeniedException(
                "Only ADMIN can delete ACL entries",
                correlationId
            );
        }

        aclService.deleteAcl(aclId, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "ACL entry deleted successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get all ACL entries for a specific account.

     * Response: 200 OK with list of AclResponseDto
     *
     * @param accountId the account ID
     * @return ApiResponse with list of ACL entries
     */
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all ACL entries for an account")
    public ResponseEntity<ApiResponse> getAclsByAccount(@PathVariable Long accountId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        //  Extract current user
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        //  Only ADMIN can delete ACL entries
        if (!currentUser.isAdmin()) {
            throw new com.example.accounts.exception.AccessDeniedException(
                    "Only ADMIN can delete ACL entries",
                    correlationId
            );
        }

        List<AclResponseDto> acls = aclService.getAclsByAccount(accountId);

        ApiResponse response = new ApiResponse(
            true,
            "ACL entries retrieved successfully",
            correlationId,
            acls,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get ACL entry for a specific user on a specific account.
     *
     * Response: 200 OK with AclResponseDto or null if not found
     *
     * @param userId the user ID
     * @param accountId the account ID
     * @return ApiResponse with ACL entry if exists
     */
    @GetMapping("/user/{userId}/account/{accountId}")
    @Operation(summary = "Get ACL entry for a user on an account")
    public ResponseEntity<ApiResponse> getAclByUserAndAccount(
            @PathVariable Long userId,
            @PathVariable Long accountId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // Extract current user
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();


        //  Only ADMIN can delete ACL entries
        if (!currentUser.isAdmin()) {
            throw new com.example.accounts.exception.AccessDeniedException(
                    "Only ADMIN can delete ACL entries",
                    correlationId
            );
        }

        AclResponseDto acl = aclService.getAclByUserAndAccount(userId, accountId);

        ApiResponse response = new ApiResponse(
            true,
            "ACL entry retrieved successfully",
            correlationId,
            acl,
            null
        );
        return ResponseEntity.ok(response);
    }
}

