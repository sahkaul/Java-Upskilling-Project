package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AssignAccountRequest", description = "Request to assign account to banker")
public class AssignAccountRequestDto {

    @NotNull(message = "Banker ID cannot be null")
    @Schema(description = "Banker ID to assign account to", example = "1")
    private Long bankerId;

    @NotNull(message = "Account ID cannot be null")
    @Schema(description = "Account ID to assign to banker", example = "1")
    private Long accountId;
}

