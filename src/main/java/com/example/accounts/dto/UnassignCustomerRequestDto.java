package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UnassignCustomerRequest", description = "Request to unassign customer from banker")
public class UnassignCustomerRequestDto {

    @NotNull(message = "Customer ID cannot be null")
    @Schema(description = "Customer ID to unassign from banker", example = "1")
    private Long customerId;
}

