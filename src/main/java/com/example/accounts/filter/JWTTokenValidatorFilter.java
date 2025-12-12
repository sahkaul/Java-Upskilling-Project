package com.example.accounts.filter;

import com.example.accounts.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT Token Validator Filter
 *
 * Validates incoming JWT tokens and populates SecurityContext with user authentication
 * This enables SecurityContextUtil.getCurrentUserContext() to work properly
 *
 * Flow:
 * 1. Extract JWT from Authorization header (Bearer token)
 * 2. Validate token signature using shared JWT_KEY
 * 3. Check token expiration
 * 4. Extract username and authorities from token claims
 * 5. Create UsernamePasswordAuthenticationToken
 * 6. Set in SecurityContextHolder for application to access
 */
@Slf4j
public class JWTTokenValidatorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract JWT from Authorization header
        // Expected format: "Bearer eyJhbGciOiJIUzI1NiJ9..."
        String jwt = request.getHeader(SecurityConstants.JWT_HEADER);

        if (jwt != null) {
            try {


                log.debug("Validating JWT token from header: Authorization");

                // ========================================================
                // STEP 1: Generate the same secret key used during token creation
                // This key MUST match the key in User microservice
                // ========================================================
                SecretKey key = Keys.hmacShaKeyFor(
                    SecurityConstants.JWT_KEY.getBytes(StandardCharsets.UTF_8));

                // ========================================================
                // STEP 2: Parse and validate JWT
                // This will throw exceptions if:
                // - Token signature is invalid
                // - Token is expired
                // - Token is malformed
                // ========================================================
                Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();

                // ========================================================
                // STEP 3: Extract claims from the JWT payload
                // These claims were added during token creation in User MS
                // ========================================================
                String username = String.valueOf(claims.get("username"));
                String authorities = (String) claims.get("authorities");

                // Extract userId from JWT claim - needed for ACL checks
                Long userId = null;
                Object userIdClaim = claims.get("userId");
                if (userIdClaim != null) {
                    userId = ((Number) userIdClaim).longValue();
                }

                log.debug("JWT validated successfully. Username: {}, UserId: {}, Authorities: {}",
                    username, userId, authorities);

                // ========================================================
                // STEP 4: Create Authentication object
                // This represents the authenticated user
                // username = email of user
                // password = null (not needed for JWT)
                // authorities = roles like "ROLE_CUSTOMER", "ROLE_BANKER", etc
                // ========================================================
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username,                                                    // principal
                    null,                                                        // credentials (not used for JWT)
                    AuthorityUtils.commaSeparatedStringToAuthorityList(authorities)  // authorities
                );

                // Store userId in authentication details so SecurityContextUtil can retrieve it
                if (userId != null) {
                    auth.setDetails(userId.toString());
                }

                // ========================================================
                // STEP 5: Set authentication in SecurityContext
                // Now SecurityContextHolder.getContext().getAuthentication()
                // will return this authentication object
                // ========================================================
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("SecurityContext populated with user authentication for: {}", username);

            } catch (io.jsonwebtoken.security.SignatureException e) {
                log.warn("JWT signature validation failed: {}", e.getMessage());
                throw new BadCredentialsException("Invalid JWT signature!");
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.warn("JWT token has expired: {}", e.getMessage());
                throw new BadCredentialsException("JWT token has expired!");
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                log.warn("JWT token is malformed: {}", e.getMessage());
                throw new BadCredentialsException("Malformed JWT token!");
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                throw new BadCredentialsException("Invalid JWT Token received!");
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Skip JWT validation for these public endpoints
        return path.equals("/actuator/health") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/favicon.ico");
    }
}

