package com.example.accounts.service.impl;

import com.example.accounts.entity.RateLimitEntry;
import com.example.accounts.exception.RateLimitExceededException;
import com.example.accounts.reository.RateLimitRepository;
import com.example.accounts.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {

    private final RateLimitRepository rateLimitRepository;

    @Value("${app.limits.customer:30}")
    private int customerLimit;

    @Value("${app.limits.banker:60}")
    private int bankerLimit;

    @Value("${app.limits.ops:120}")
    private int opsLimit;

    private static final long WINDOW_SIZE = 60 * 1000; // 1 minute in milliseconds

    @Override
    @Transactional
    public boolean isRateLimited(Long userId, String endpoint) {
        long now = System.currentTimeMillis();
        long windowStart = now - (now % WINDOW_SIZE);
        long windowEnd = windowStart + WINDOW_SIZE;

        Optional<RateLimitEntry> existing = rateLimitRepository
            .findByUserIdAndEndpointAndWindowStart(userId, endpoint, windowStart);

        if (existing.isEmpty()) {
            return false; // First request in window
        }

        RateLimitEntry entry = existing.get();
        int limit = getLimit(userId);

        return entry.getRequestCount() >= limit;
    }

    @Override
    @Transactional
    public void recordRequest(Long userId, String endpoint) {
        long now = System.currentTimeMillis();
        long windowStart = now - (now % WINDOW_SIZE);
        long windowEnd = windowStart + WINDOW_SIZE;

        Optional<RateLimitEntry> existing = rateLimitRepository
            .findByUserIdAndEndpointAndWindowStart(userId, endpoint, windowStart);

        if (existing.isPresent()) {
            RateLimitEntry entry = existing.get();
            entry.setRequestCount(entry.getRequestCount() + 1);
            rateLimitRepository.save(entry);
        } else {
            RateLimitEntry entry = new RateLimitEntry();
            entry.setUserId(userId);
            entry.setEndpoint(endpoint);
            entry.setRequestCount(1);
            entry.setWindowStart(windowStart);
            entry.setWindowEnd(windowEnd);
            rateLimitRepository.save(entry);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getRetryAfterSeconds(Long userId, String endpoint) {
        long now = System.currentTimeMillis();
        long windowStart = now - (now % WINDOW_SIZE);

        Optional<RateLimitEntry> existing = rateLimitRepository
            .findByUserIdAndEndpointAndWindowStart(userId, endpoint, windowStart);

        if (existing.isEmpty()) {
            return 0;
        }

        RateLimitEntry entry = existing.get();
        long secondsUntilNextWindow = (entry.getWindowEnd() - now) / 1000;
        return Math.max(secondsUntilNextWindow, 1);
    }

    private int getLimit(Long userId) {
        // Default to customer limit; in production, check user role
        return customerLimit;
    }
}

