package com.example.accounts.dto;

import com.example.accounts.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Accounts",
        description = "Schema to hold Account information"
)
public class AccountsDto {

    private Long accountId;

    private Long customerId;

    private String accountNumber;

    @NotEmpty(message = "AccountType can not be a null or empty")
    @Schema(
            description = "Account type of FinBankX account", example = "SAVINGS"
    )
    private String accountType;

    @Schema(
            description = "Account Status", example = "ACTIVE"
    )
    private String accountStatus;

    @Schema(
            description = "Account Balance", example = "10000.00"
    )
    private BigDecimal balance;

    @Schema(
            description = "Currency", example = "USD"
    )
    private String currency;

    @NotEmpty(message = "BranchAddress can not be a null or empty")
    @Schema(
            description = "FinBankX branch address", example = "123 NewYork"
    )
    private String branchAddress;

    private String maskedAccountNumber;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String frozenReason;

    private String closedReason;
}
