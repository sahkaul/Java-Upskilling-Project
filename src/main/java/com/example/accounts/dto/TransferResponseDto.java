package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TransferResponse", description = "Schema for transfer response")
public class TransferResponseDto {

    private Long transferId;

    private Long sourceAccountId;

    private Long destinationAccountId;

    private BigDecimal amount;

    private String currency;

    private String transferStatus;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime authorizedAt;

    private LocalDateTime postedAt;

    private String idempotencyKey;

    private String correlationId;
}

