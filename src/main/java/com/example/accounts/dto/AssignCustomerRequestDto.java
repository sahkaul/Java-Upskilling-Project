package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AssignCustomerRequest", description = "Request to assign customer to banker")
public class AssignCustomerRequestDto {

    @NotNull(message = "Banker ID cannot be null")
    @Schema(description = "Banker ID to assign customer to", example = "1")
    private Long bankerId;

    @NotNull(message = "Customer ID cannot be null")
    @Schema(description = "Customer ID to assign to banker", example = "1")
    private Long customerId;
}

