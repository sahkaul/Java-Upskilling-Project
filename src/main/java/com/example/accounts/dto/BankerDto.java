package com.example.accounts.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for Banker entity
 * Used for API requests and responses
 *
 * Note: userId is extracted from JWT token in controller,
 * not passed in request body
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankerDto {

    private Long bankerId;

    private Long userId;  // Read-only: extracted from JWT during creation

    private String branchCode;

    private String portfolio;

    private Boolean isActive;


    private Integer assignedAccountCount;

    private Integer assignedCustomerCount;

    private List<Long> assignedAccountIds;

    private List<Long> assignedCustomerIds;

    private String createdAt;

    private String modifiedAt;
}

