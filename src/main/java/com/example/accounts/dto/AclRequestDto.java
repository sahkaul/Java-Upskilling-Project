package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AclRequest", description = "Request for ACL operations (add/update)")
public class AclRequestDto {

    @NotNull(message = "Account ID cannot be null")
    @Schema(description = "Account ID", example = "1")
    private Long accountId;

    @NotNull(message = "User ID cannot be null")
    @Schema(description = "User ID (reference to User microservice)", example = "5")
    private Long userId;

    @NotNull(message = "Permission cannot be null")
    @Schema(description = "Permission to grant", example = "VIEW",
            allowableValues = {"VIEW", "UPDATE", "DELETE", "TRANSFER"})
    private String permission;
}

