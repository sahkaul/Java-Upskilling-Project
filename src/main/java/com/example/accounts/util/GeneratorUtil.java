package com.example.accounts.util;

import java.security.MessageDigest;
import java.util.UUID;

public class GeneratorUtil {

    public static String generateAccountNumber() {
        return "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    public static String generateLedgerTransactionId() {
        return "LTX" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public static String hashRequest(String requestBody) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(requestBody.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

