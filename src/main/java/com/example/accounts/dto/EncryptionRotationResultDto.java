package com.example.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for encryption rotation result details
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EncryptionRotationResultDto {

    private boolean success;
    private String message;
    private Long totalProcessed;
    private Long successCount;
    private Long failureCount;
    private Integer previousEncryptionVersion;
    private Integer newEncryptionVersion;
    private LocalDateTime rotationStartTime;
    private LocalDateTime rotationEndTime;
    private Long durationMillis;
    private String entityType; // CUSTOMER, ACCOUNT, etc.
}

