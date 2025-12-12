package com.example.accounts.service;

import com.example.accounts.entity.AccessControlList;

import java.util.List;

public interface AccessControlService {
    void grantAccess(Long accountId, Long userId, AccessControlList.Permission permission);

    void revokeAccess(Long accountId, Long userId);

    boolean hasPermission(Long accountId, Long userId, AccessControlList.Permission permission);

    List<AccessControlList> getAccountAccess(Long accountId);

    void grantCustomerWideAccess(Long customerId, Long userId, AccessControlList.Permission permission);
}

