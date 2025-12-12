package com.example.accounts.constants;

/**
 * Security constants for JWT token handling
 * IMPORTANT: JWT_KEY must be the same as in User microservice for token validation
 */
public class SecurityConstants {

    /**
     * Secret key for JWT token signing and verification
     * MUST match the key in User microservice: com.example.user.constants.SecurityConstants.JWT_KEY
     */
    public static final String JWT_KEY = "jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4";

    /**
     * HTTP header name for JWT token
     */
    public static final String JWT_HEADER = "Authorization";

}

