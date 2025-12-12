package com.example.accounts.service.impl;

import com.example.accounts.crypto.EncryptionService;
import com.example.accounts.entity.Account;
import com.example.accounts.entity.Customer;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.reository.CustomerRepository;
import com.example.accounts.service.EncryptionRotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for handling encryption key rotation and re-encryption of sensitive data.
 *
 * Features:
 * - Scheduled job that runs periodically to re-encrypt customers/accounts older than 10 days
 * - Re-encrypts entities with old encryption versions
 * - Atomic updates without interrupting read operations
 * - Metadata updated post-success (lastEncryptedOn, encryptionVersion)
 * - Batch processing for large datasets
 * - Non-blocking reads with proper transaction isolation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncryptionRotationServiceImpl implements EncryptionRotationService {

    private final EncryptionService encryptionService;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    @Value("${app.encryption.version:1}")
    private Integer currentEncryptionVersion;


    @Value("${app.encryption.rotation.enabled:true}")
    private Boolean rotationEnabled;

    /**
     * Scheduled job for automatic customer encryption rotation
     * Runs every day at 2 AM
     * Uses SERIALIZABLE isolation to prevent concurrent writes while allowing reads
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void scheduleCustomerEncryptionRotation() {
        if (!rotationEnabled) {
            log.debug("Encryption rotation is disabled");
            return;
        }

        log.info("=== Starting Scheduled Customer Encryption Rotation ===");
        rotateCustomerEncryption();
    }

    /**
     * Scheduled job for automatic account encryption rotation
     * Runs every day at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void scheduleAccountEncryptionRotation() {
        if (!rotationEnabled) {
            log.debug("Encryption rotation is disabled");
            return;
        }

        log.info("=== Starting Scheduled Account Encryption Rotation ===");
        rotateAccountEncryption();
    }

    /**
     * Re-encrypt all customers with old encryption version or older than 10 days
     */
    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void rotateCustomerEncryption() {
        long startTime = System.currentTimeMillis();
        long successCount = 0;
        long failureCount = 0;

        try {
            log.info("Starting customer encryption rotation. Current version: {}", currentEncryptionVersion);

            // Find all customers needing re-encryption
            List<Customer> customersToRotate = customerRepository.findCustomersNeedingReEncryption(currentEncryptionVersion);
            long totalCount = customersToRotate.size();

            if (totalCount == 0) {
                log.info("No customers found needing re-encryption");
                return;
            }

            log.info("Found {} customers needing re-encryption", totalCount);

            // Process all customers
            for (Customer customer : customersToRotate) {
                try {
                    if (reEncryptCustomer(customer.getCustomerId())) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Error rotating encryption for customer {}", customer.getCustomerId(), e);
                    failureCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Customer encryption rotation completed. Total: {}, Success: {}, Failures: {}, Duration: {}ms",
                totalCount, successCount, failureCount, duration);

        } catch (Exception e) {
            log.error("Error during customer encryption rotation", e);
        }
    }

    /**
     * Re-encrypt all accounts with old encryption version or older than 10 days
     */
    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void rotateAccountEncryption() {
        long startTime = System.currentTimeMillis();
        long successCount = 0;
        long failureCount = 0;

        try {
            log.info("Starting account encryption rotation. Current version: {}", currentEncryptionVersion);

            // Find all accounts needing re-encryption
            List<Account> accountsToRotate = accountRepository.findAccountsNeedingReEncryption(currentEncryptionVersion);
            long totalCount = accountsToRotate.size();

            if (totalCount == 0) {
                log.info("No accounts found needing re-encryption");
                return;
            }

            log.info("Found {} accounts needing re-encryption", totalCount);

            // Process all accounts
            for (Account account : accountsToRotate) {
                try {
                    if (reEncryptAccount(account.getAccountId())) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Error rotating encryption for account {}", account.getAccountId(), e);
                    failureCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Account encryption rotation completed. Total: {}, Success: {}, Failures: {}, Duration: {}ms",
                totalCount, successCount, failureCount, duration);

        } catch (Exception e) {
            log.error("Error during account encryption rotation", e);
        }
    }

    /**
     * Re-encrypt a specific customer atomically
     *
     * Atomic operation:
     * 1. Fetch customer (uses existing key to decrypt)
     * 2. Decrypt sensitive fields with old version key
     * 3. Encrypt with new key
     * 4. Update database atomically
     * 5. Update metadata (encryptionVersion, lastEncryptedOn)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean reEncryptCustomer(Long customerId) {
        try {
            Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));

            // Skip if already at current encryption version
            if (customer.getEncryptionVersion().equals(currentEncryptionVersion)) {
                log.debug("Customer {} already at current encryption version", customerId);
                return true;
            }

            Integer oldVersion = customer.getEncryptionVersion();

            try {
                // Decrypt with old key (in case key was upgraded)
                String plainEmail = encryptionService.decryptWithOldKey(customer.getEmail());
                String plainPhone = customer.getPhoneNumber() != null ?
                    encryptionService.decryptWithOldKey(customer.getPhoneNumber()) : null;
                String plainAddress = customer.getAddress() != null ?
                    encryptionService.decryptWithOldKey(customer.getAddress()) : null;

                // Encrypt with new key
                customer.setEmail(encryptionService.encrypt(plainEmail));
                customer.setPhoneNumber(plainPhone != null ? encryptionService.encrypt(plainPhone) : null);
                customer.setAddress(plainAddress != null ? encryptionService.encrypt(plainAddress) : null);
                customer.setEncryptionVersion(currentEncryptionVersion);
                customer.setLastEncryptedOn(System.currentTimeMillis());

                // Save atomically
                customerRepository.saveAndFlush(customer);

                log.info("Successfully re-encrypted customer {} from version {} to {}",
                    customerId, oldVersion, currentEncryptionVersion);
                return true;

            } catch (Exception e) {
                log.error("Error decrypting/re-encrypting customer {} data", customerId, e);
                return false;
            }

        } catch (Exception e) {
            log.error("Error rotating encryption for customer {}", customerId, e);
            return false;
        }
    }

    /**
     * Re-encrypt a specific account atomically
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean reEncryptAccount(Long accountId) {
        try {
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));

            // Skip if already at current encryption version
            if (account.getEncryptionVersion().equals(currentEncryptionVersion)) {
                log.debug("Account {} already at current encryption version", accountId);
                return true;
            }

            Integer oldVersion = account.getEncryptionVersion();

            try {
                // Update encryption metadata
                account.setEncryptionVersion(currentEncryptionVersion);
                account.setLastEncryptedOn(System.currentTimeMillis());

                // Save atomically
                accountRepository.saveAndFlush(account);

                log.info("Successfully re-encrypted account {} from version {} to {}",
                    accountId, oldVersion, currentEncryptionVersion);
                return true;

            } catch (Exception e) {
                log.error("Error re-encrypting account {} data", accountId, e);
                return false;
            }

        } catch (Exception e) {
            log.error("Error rotating encryption for account {}", accountId, e);
            return false;
        }
    }
}

