package com.example.accounts.service;

import com.example.accounts.dto.TransferRequestDto;

public interface IdempotencyService {
    boolean checkIdempotency(String idempotencyKey, String requestHash, Long userId);

    String getIdempotentResponse(String idempotencyKey);

    void storeIdempotentResponse(String idempotencyKey, String requestHash, Integer statusCode, String response, Long userId);

    void cleanupExpiredKeys();

    /**
     * Generate SHA256 hash from TransferRequestDto for idempotency checking.
     * Hash is based on: sourceAccountId | destinationAccountId | amount
     *
     * @param request the transfer request
     * @return SHA256 hash of the request
     */
    String generateRequestHash(TransferRequestDto request);

    /**
     * Retrieve and deserialize cached response from idempotency store.
     * Automatically deserializes JSON string back to the original response object.
     *
     * @param idempotencyKey the idempotency key
     * @param <T> the response type
     * @return the deserialized response object, or null if not found
     */
    <T> T getAndDeserializeResponse(String idempotencyKey, Class<T> responseType);

    /**
     * Serialize response object to JSON and store in idempotency cache.
     * Generates request hash and stores both response and hash for duplicate detection.
     *
     * @param idempotencyKey the unique idempotency key from request
     * @param request the original transfer request (used to generate hash)
     * @param response the response object to serialize and cache
     * @param statusCode HTTP status code (e.g., 201 for Created)
     * @param userId the user ID who made the request
     * @throws RuntimeException if serialization or storage fails
     */
    void serializeAndStoreResponse(String idempotencyKey, TransferRequestDto request, Object response, Integer statusCode, Long userId);
}

