package com.example.accounts.exception;

public class AccessDeniedException extends RuntimeException {
    private String correlationId;

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}

