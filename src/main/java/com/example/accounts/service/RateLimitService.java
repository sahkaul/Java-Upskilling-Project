package com.example.accounts.service;

public interface RateLimitService {
    boolean isRateLimited(Long userId, String endpoint);

    void recordRequest(Long userId, String endpoint);

    long getRetryAfterSeconds(Long userId, String endpoint);
}

