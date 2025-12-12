package com.example.accounts.util;

public class MaskingUtil {

    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        String localPart = parts[0];
        String domain = parts[1];

        String maskedLocal = localPart.length() > 2
            ? localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1)
            : localPart;

        String maskedDomain = domain.length() > 4
            ? domain.substring(0, 2) + "***" + domain.substring(domain.length() - 2)
            : domain;

        return maskedLocal + "@" + maskedDomain;
    }

    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty() || phoneNumber.length() < 4) {
            return phoneNumber;
        }
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return "+" + "*".repeat(phoneNumber.length() - 4) + lastFour;
    }

    public static String maskAddress(String address) {
        if (address == null || address.isEmpty() || address.length() < 4) {
            return address;
        }
        String lastFour = address.substring(address.length() - 4);
        return "*".repeat(Math.max(address.length() - 4, 5)) + lastFour;
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 6) {
            return accountNumber;
        }
        String firstFour = accountNumber.substring(0, 4);
        String lastTwo = accountNumber.substring(accountNumber.length() - 2);
        return firstFour + "****" + lastTwo;
    }
}

