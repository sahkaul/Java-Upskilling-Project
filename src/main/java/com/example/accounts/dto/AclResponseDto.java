package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AclResponse", description = "Response for ACL operations")
public class AclResponseDto {

    @Schema(description = "ACL ID", example = "1")
    private Long aclId;

    @Schema(description = "Account ID", example = "1")
    private Long accountId;

    @Schema(description = "User ID (reference to User microservice)", example = "5")
    private Long userId;

    @Schema(description = "Permission granted", example = "VIEW")
    private String permission;

    @Schema(description = "When this ACL entry was created")
    private LocalDateTime createdAt;

    @Schema(description = "When this ACL entry was last updated")
    private LocalDateTime updatedAt;
}

