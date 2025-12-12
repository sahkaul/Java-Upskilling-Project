package com.example.accounts.exception;

public class RateLimitExceededException extends RuntimeException {
    private long retryAfter;

    public RateLimitExceededException(String message, long retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public long getRetryAfter() {
        return retryAfter;
    }
}

