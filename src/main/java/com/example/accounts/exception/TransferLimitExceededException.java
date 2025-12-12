package com.example.accounts.exception;

public class TransferLimitExceededException extends RuntimeException {
    private String errorCode;

    public TransferLimitExceededException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

