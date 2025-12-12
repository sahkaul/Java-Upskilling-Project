package com.example.accounts.exception;

import com.example.accounts.dto.ApiResponse;
import com.example.accounts.util.GeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            correlationId,
            null,
            "BAD_CREDENTIALS"
        );
        log.warn("Bad credentials/Invalid JWT. CorrelationId: {}", correlationId);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            correlationId,
            null,
            "RESOURCE_NOT_FOUND"
        );
        log.warn("Resource not found. CorrelationId: {}", correlationId);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        String correlationId = ex.getCorrelationId() != null ? ex.getCorrelationId() : GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            correlationId,
            null,
            "ACCESS_DENIED"
        );
        log.warn("Access denied. CorrelationId: {}", correlationId);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse> handleInsufficientFunds(InsufficientFundsException ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            correlationId,
            null,
            "INSUFFICIENT_FUNDS"
        );
        log.warn("Insufficient funds. CorrelationId: {}", correlationId);
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(TransferLimitExceededException.class)
    public ResponseEntity<ApiResponse> handleLimitExceeded(TransferLimitExceededException ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            correlationId,
            null,
            ex.getErrorCode()
        );
        log.warn("Transfer limit exceeded. CorrelationId: {}", correlationId);
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ApiResponse> handleInvalidTransfer(InvalidTransferException ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            correlationId,
            null,
            "INVALID_TRANSFER"
        );
        log.warn("Invalid transfer. CorrelationId: {}", correlationId);
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiResponse> handleIdempotencyConflict(IdempotencyConflictException ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            correlationId,
            null,
            "IDEMPOTENCY_CONFLICT"
        );
        log.warn("Idempotency conflict. CorrelationId: {}", correlationId);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse> handleRateLimitExceeded(RateLimitExceededException ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            ex.getMessage(),
            correlationId,
            null,
            "RATE_LIMIT_EXCEEDED"
        );
        log.warn("Rate limit exceeded. CorrelationId: {}", correlationId);
        ResponseEntity<ApiResponse> entity = new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
        return entity;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Validation failed");

        ApiResponse response = new ApiResponse(
            false,
            message,
            correlationId,
            null,
            "VALIDATION_ERROR"
        );
        log.warn("Validation error. CorrelationId: {}", correlationId);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex, WebRequest request) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        ApiResponse response = new ApiResponse(
            false,
            "An unexpected error occurred",
            correlationId,
            null,
            "INTERNAL_SERVER_ERROR"
        );
        log.error("Unexpected error. CorrelationId: {}", correlationId, ex);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

