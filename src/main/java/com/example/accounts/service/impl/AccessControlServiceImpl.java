package com.example.accounts.service.impl;

import com.example.accounts.entity.AccessControlList;
import com.example.accounts.entity.Account;
import com.example.accounts.entity.Customer;
import com.example.accounts.exception.ResourceNotFoundException;
import com.example.accounts.reository.AccessControlListRepository;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.reository.CustomerRepository;
import com.example.accounts.service.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessControlServiceImpl implements AccessControlService {

    private final AccessControlListRepository aclRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void grantAccess(Long accountId, Long userId, AccessControlList.Permission permission) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccessControlList acl = new AccessControlList();
        acl.setAccount(account);
        acl.setUserId(userId);
        acl.setPermission(permission);

        aclRepository.save(acl);
        log.info("Access granted. Account: {}, User: {}, Permission: {}", accountId, userId, permission);
    }

    @Override
    @Transactional
    public void revokeAccess(Long accountId, Long userId) {
        AccessControlList acl = aclRepository.findByAccountAccountIdAndUserId(accountId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Access control entry not found"));

        aclRepository.delete(acl);
        log.info("Access revoked. Account: {}, User: {}", accountId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Long accountId, Long userId, AccessControlList.Permission permission) {
        if (!aclRepository.existsByAccountAccountIdAndUserId(accountId, userId)) {
            return false;
        }

        AccessControlList acl = aclRepository.findByAccountAccountIdAndUserId(accountId, userId)
            .orElse(null);

        return acl != null && acl.getPermission() == permission;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessControlList> getAccountAccess(Long accountId) {
        return aclRepository.findByAccountAccountId(accountId);
    }

    @Override
    @Transactional
    public void grantCustomerWideAccess(Long customerId, Long userId, AccessControlList.Permission permission) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        AccessControlList acl = new AccessControlList();
        acl.setCustomer(customer);
        acl.setUserId(userId);
        acl.setPermission(permission);

        aclRepository.save(acl);
        log.info("Customer-wide access granted. Customer: {}, User: {}, Permission: {}", customerId, userId, permission);
    }
}

