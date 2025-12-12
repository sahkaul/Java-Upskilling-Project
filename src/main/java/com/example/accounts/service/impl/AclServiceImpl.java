package com.example.accounts.service.impl;

import com.example.accounts.dto.AclRequestDto;
import com.example.accounts.dto.AclResponseDto;
import com.example.accounts.entity.AccessControlList;
import com.example.accounts.entity.Account;
import com.example.accounts.exception.ResourceNotFoundException;
import com.example.accounts.exception.AccessDeniedException;
import com.example.accounts.reository.AccessControlListRepository;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.service.AclService;
import com.example.accounts.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing Access Control Lists (ACL).
 * Handles granting, updating, and removing permissions for users on accounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AclServiceImpl implements AclService {

    private final AccessControlListRepository aclRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public AclResponseDto addAcl(AclRequestDto request, String correlationId) {
        // Validate account exists
        Account account = accountRepository.findById(request.getAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + request.getAccountId()));

        // Check if ACL already exists for this user and account
        if (aclRepository.existsByAccountAccountIdAndUserId(request.getAccountId(), request.getUserId())) {
            throw new IllegalArgumentException(
                "ACL entry already exists for user " + request.getUserId() + " on account " + request.getAccountId()
            );
        }

        // Validate permission is valid
        AccessControlList.Permission permission;
        try {
            permission = AccessControlList.Permission.valueOf(request.getPermission().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid permission: " + request.getPermission());
        }

        // Create ACL entry
        AccessControlList acl = new AccessControlList();
        acl.setAccount(account);
        acl.setUserId(request.getUserId());
        acl.setPermission(permission);

        AccessControlList saved = aclRepository.save(acl);

        log.info("ACL entry created. UserId: {}, AccountId: {}, Permission: {}, CorrelationId: {}",
            request.getUserId(), request.getAccountId(), permission, correlationId);

        return convertToDto(saved);
    }

    @Override
    @Transactional
    public AclResponseDto updateAcl(Long aclId, AclRequestDto request, String correlationId) {
        // Get ACL entry
        AccessControlList acl = aclRepository.findById(aclId)
            .orElseThrow(() -> new ResourceNotFoundException("ACL entry not found with ID: " + aclId));

        // Validate permission is valid
        AccessControlList.Permission permission;
        try {
            permission = AccessControlList.Permission.valueOf(request.getPermission().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid permission: " + request.getPermission());
        }

        // If trying to change account or userId, prevent it (not allowed in update)
        if (!acl.getAccount().getAccountId().equals(request.getAccountId())) {
            throw new IllegalArgumentException("Cannot change account in update operation");
        }
        if (!acl.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("Cannot change user ID in update operation");
        }

        // Update permission
        AccessControlList.Permission oldPermission = acl.getPermission();
        acl.setPermission(permission);

        AccessControlList saved = aclRepository.save(acl);

        log.info("ACL entry updated. AclId: {}, Permission: {} → {}, CorrelationId: {}",
            aclId, oldPermission, permission, correlationId);

        return convertToDto(saved);
    }

    @Override
    @Transactional
    public void deleteAcl(Long aclId, String correlationId) {
        // Get ACL entry
        AccessControlList acl = aclRepository.findById(aclId)
            .orElseThrow(() -> new ResourceNotFoundException("ACL entry not found with ID: " + aclId));

        // Get details for logging
        Long userId = acl.getUserId();
        Long accountId = acl.getAccount().getAccountId();
        AccessControlList.Permission permission = acl.getPermission();

        // Delete ACL entry
        aclRepository.delete(acl);

        log.info("ACL entry deleted. AclId: {}, UserId: {}, AccountId: {}, Permission: {}, CorrelationId: {}",
            aclId, userId, accountId, permission, correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AclResponseDto> getAclsByAccount(Long accountId) {
        // Validate account exists
        accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        // Get all ACL entries for this account
        List<AccessControlList> acls = aclRepository.findByAccountAccountId(accountId);

        return acls.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AclResponseDto getAclByUserAndAccount(Long userId, Long accountId) {
        return aclRepository.findByAccountAccountIdAndUserId(accountId, userId)
            .map(this::convertToDto)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, Long accountId, AccessControlList.Permission permission) {
        return aclRepository.findByAccountAccountIdAndUserId(accountId, userId)
            .map(acl -> acl.getPermission() == permission)
            .orElse(false);
    }

    /**
     * Convert AccessControlList entity to AclResponseDto
     */
    private AclResponseDto convertToDto(AccessControlList acl) {
        return new AclResponseDto(
            acl.getAclId(),
            acl.getAccount().getAccountId(),
            acl.getUserId(),
            acl.getPermission().toString(),
            acl.getCreatedAt(),
            acl.getUpdatedAt()
        );
    }
}

