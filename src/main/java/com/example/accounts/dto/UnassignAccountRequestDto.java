package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UnassignAccountRequest", description = "Request to unassign account from banker")
public class UnassignAccountRequestDto {

    @NotNull(message = "Account ID cannot be null")
    @Schema(description = "Account ID to unassign from banker", example = "1")
    private Long accountId;
}

