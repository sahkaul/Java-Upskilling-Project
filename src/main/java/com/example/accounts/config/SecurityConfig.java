package com.example.accounts.config;

import com.example.accounts.filter.JWTTokenValidatorFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Spring Security Configuration for Accounts Microservice
 *
 * This configuration:
 * 1. Enables Spring Security
 * 2. Registers JWTTokenValidatorFilter to validate incoming JWT tokens
 * 3. Sets session policy to STATELESS (microservices don't use sessions)
 * 4. Requires authentication for all endpoints
 *
 * Flow:
 * - JWT Filter validates token before request reaches controller
 * - SecurityContext is populated with user info
 * - Controllers can now use SecurityContextUtil to get current user
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ================================================
            // CSRF Protection: Disabled
            // Reason: Microservice uses stateless authentication (JWT)
            // CSRF protection is only needed for session-based auth
            // ================================================
            .csrf(csrf -> csrf.disable())

            // ================================================
            // Authorization Rules (Spring Security 6.1+ syntax)
            // All requests require authentication
            // ================================================
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            )

            // ================================================
            // Session Management: STATELESS
            // Reason: JWT is stateless, each request is independent
            // No session storage needed
            // ================================================
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ================================================
            // Add JWT Validator Filter
            // Place it BEFORE BasicAuthenticationFilter
            // This ensures JWT validation happens first
            // If JWT is valid, SecurityContext is populated
            // Controllers can then access user info via SecurityContextUtil
            // ================================================
            .addFilterBefore(new JWTTokenValidatorFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }
}



