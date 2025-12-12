package com.example.accounts.util;

import com.example.accounts.dto.CurrentUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class to extract current authenticated user information from JWT token.
 *
 * The JWT token contains:
 * - principal: username (email)
 * - userId: userId from User microservice
 * - roles: list of roles (ROLE_CUSTOMER, ROLE_BANKER, ROLE_OPS, ROLE_ADMIN)
 *
 * Note: CustomerId is NOT extracted here. It will be fetched from Customer table
 * in AuthorizationServiceImpl when needed for ACL checks using userId as lookup key.
 *
 * This separation of concerns makes the code cleaner:
 * - SecurityContextUtil: Only extracts info available in JWT
 * - AuthorizationServiceImpl: Fetches additional data (customerId) when needed
 */
@Component
@Slf4j
public class SecurityContextUtil {

    /**
     * Extract current user context from JWT token stored in SecurityContext.
     *
     * Flow:
     * 1. Extract userId from JWT token (stored in authentication details by JWTTokenValidatorFilter)
     * 2. Extract roles from authentication authorities
     * 3. Return CurrentUserContext with userId and roles
     *
     * CustomerId is NOT extracted here - it will be fetched by AuthorizationServiceImpl
     * when needed for ACL checks.
     *
     * @return CurrentUserContext with userId and roles
     * @throws IllegalStateException if no authentication found
     */
    public static CurrentUserContext getCurrentUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found in SecurityContext");
            throw new IllegalStateException("User is not authenticated");
        }

        // ================================================
        // STEP 1: Extract userId from JWT token
        // (stored by JWTTokenValidatorFilter in auth details)
        // ================================================
        Long userId = extractUserId(authentication);

        // ================================================
        // STEP 2: Extract roles from authentication
        // ================================================
        java.util.Set<String> roles = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(java.util.stream.Collectors.toSet());

        log.debug("Current user context - UserId: {}, Roles: {}",
            userId, roles);

        // ================================================
        // STEP 3: Return CurrentUserContext
        // Note: customerId NOT included - will be fetched by AuthorizationServiceImpl
        // ================================================
        return new CurrentUserContext(userId, roles);
    }

    /**
     * Extract userId from authentication details.
     * The JWTTokenValidatorFilter stores userId in the details field.
     *
     * @return userId extracted from authentication details
     */
    private static Long extractUserId(Authentication authentication) {
        try {
            Object details = authentication.getDetails();

            // Details should contain userId (stored by JWTTokenValidatorFilter)
            if (details != null && details instanceof String) {
                String userIdStr = (String) details;
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    log.warn("UserId in authentication details is not a valid number: {}", userIdStr);
                    return null;
                }
            }

            log.warn("UserId not found in authentication details");
            return null;

        } catch (Exception e) {
            log.warn("Failed to extract userId from authentication details: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if current user has a specific role.
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals(role));
    }

    /**
     * Check if current user is a CUSTOMER.
     */
    public static boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    /**
     * Check if current user is a BANKER.
     */
    public static boolean isBanker() {
        return hasRole("BANKER");
    }

    /**
     * Check if current user is OPS.
     */
    public static boolean isOps() {
        return hasRole("OPS");
    }

    /**
     * Check if current user is ADMIN.
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

}

