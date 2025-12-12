package com.example.accounts.exception;

/**
 * Exception thrown when there's an invalid banker operation
 */
public class InvalidBankerException extends RuntimeException {

    private String errorCode;

    public InvalidBankerException(String message) {
        super(message);
        this.errorCode = "INVALID_BANKER_OPERATION";
    }

    public InvalidBankerException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidBankerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_BANKER_OPERATION";
    }

    public String getErrorCode() {
        return errorCode;
    }
}

