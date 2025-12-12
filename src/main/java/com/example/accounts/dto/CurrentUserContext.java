package com.example.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Data Transfer Object representing the current authenticated user's context.
 * Extracted from JWT token claims.
 *
 * Note: customerId is NOT stored here. It will be fetched from Customer table
 * in AuthorizationServiceImpl when needed for ACL checks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserContext {

    /**
     * User ID from User microservice (from JWT token)
     */
    private Long userId;

    /**
     * Set of roles assigned to this user
     * Examples: ROLE_CUSTOMER, ROLE_BANKER, ROLE_OPS, ROLE_ADMIN
     */
    private Set<String> roles;

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        return roles.stream()
            .anyMatch(r -> r.equalsIgnoreCase(role) || r.equals("ROLE_" + role));
    }

    /**
     * Check if user has CUSTOMER role
     */
    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    /**
     * Check if user has BANKER role
     */
    public boolean isBanker() {
        return hasRole("BANKER");
    }

    /**
     * Check if user has OPS role
     */
    public boolean isOps() {
        return hasRole("OPS");
    }

    /**
     * Check if user has ADMIN role
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}

