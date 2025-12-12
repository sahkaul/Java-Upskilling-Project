package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TransferRequest", description = "Schema for initiating a transfer")
public class TransferRequestDto {

    @NotNull(message = "Source account ID cannot be null")
    @Schema(description = "Source account ID", example = "1")
    private Long sourceAccountId;

    @NotNull(message = "Destination account ID cannot be null")
    @Schema(description = "Destination account ID", example = "2")
    private Long destinationAccountId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "Transfer amount", example = "1000.50")
    private BigDecimal amount;

    @Schema(description = "Transfer description", example = "Payment for services")
    private String description;

    @Schema(description = "Idempotency key for request deduplication")
    private String idempotencyKey;
}

