package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateAccountForCustomerRequest", description = "Request to create account for assigned customer")
public class CreateAccountForCustomerRequestDto {

    @NotNull(message = "Customer ID cannot be null")
    @Schema(description = "Customer ID to create account for", example = "1")
    private Long customerId;

    @NotNull(message = "Account type cannot be null")
    @Schema(description = "Type of account", example = "SAVINGS", allowableValues = {"SAVINGS", "CHECKING", "BUSINESS"})
    private String accountType;

    @Schema(description = "Currency for the account", example = "USD")
    private String currency;

    @Schema(description = "Branch address", example = "123 Main Street, New York")
    private String branchAddress;
}

