package com.example.accounts.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionService {

    @Value("${app.encryption.key:finbankx-enc-key-32chars-exactly}")
    private String encryptionKey;

    @Value("${app.encryption.algorithm:AES/GCM/NoPadding}")
    private String algorithm;

    @Value("${app.encryption.old-key:}")
    private String oldEncryptionKey;

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;


    public String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        byte[] key = encryptionKey.getBytes();
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(key, 0, key.length, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encryptedData = cipher.doFinal(plainText.getBytes());

        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        byte[] combined = Base64.getDecoder().decode(encryptedText);
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), 0, encryptionKey.getBytes().length, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] decryptedData = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
        return new String(decryptedData);
    }

    /**
     * Decrypt data using old encryption key
     * Used during key rotation to decrypt data encrypted with old key
     */
    public String decryptWithOldKey(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        // Use old key if configured, otherwise use current key
        String keyToUse = (oldEncryptionKey != null && !oldEncryptionKey.isEmpty())
            ? oldEncryptionKey
            : encryptionKey;

        byte[] combined = Base64.getDecoder().decode(encryptedText);
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        SecretKeySpec keySpec = new SecretKeySpec(keyToUse.getBytes(), 0, keyToUse.getBytes().length, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] decryptedData = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
        return new String(decryptedData);
    }
}

