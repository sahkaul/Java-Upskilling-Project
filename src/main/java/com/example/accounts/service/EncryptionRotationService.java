package com.example.accounts.service;

/**
 * Service for handling encryption key rotation and re-encryption of sensitive data
 */
public interface EncryptionRotationService {

    /**
     * Re-encrypt all customers with old encryption version or older than 10 days
     */
    void rotateCustomerEncryption();

    /**
     * Re-encrypt all accounts with old encryption version or older than 10 days
     */
    void rotateAccountEncryption();
}

