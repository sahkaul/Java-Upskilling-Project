package com.example.accounts.service.impl;

import com.example.accounts.dto.TransferRequestDto;
import com.example.accounts.entity.IdempotencyKey;
import com.example.accounts.exception.IdempotencyConflictException;
import com.example.accounts.reository.IdempotencyKeyRepository;
import com.example.accounts.service.IdempotencyService;
import com.example.accounts.util.GeneratorUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private static final long TTL_24_HOURS = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    @Override
    @Transactional(readOnly = true)
    public boolean checkIdempotency(String idempotencyKey, String requestHash, Long userId) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isEmpty()) {
            return false; // New request
        }

        IdempotencyKey idemKey = existing.get();

        // Check if key is expired
        if (System.currentTimeMillis() > idemKey.getExpiresAt()) {
            return false; // Key expired
        }

        // Check if request hash matches
        if (!idemKey.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("Idempotency key used with different request");
        }

        return true; // Duplicate request with same hash
    }

    @Override
    @Transactional(readOnly = true)
    public String getIdempotentResponse(String idempotencyKey) {
        Optional<IdempotencyKey> idemKey = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);
        return idemKey.map(IdempotencyKey::getResponseBody).orElse(null);
    }

    @Override
    @Transactional
    public void storeIdempotentResponse(String idempotencyKey, String requestHash, Integer statusCode, String response, Long userId) {
        IdempotencyKey idemKey = new IdempotencyKey();
        idemKey.setIdempotencyKey(idempotencyKey);
        idemKey.setRequestHash(requestHash);
        idemKey.setResponseStatus(statusCode);
        idemKey.setResponseBody(response);
        idemKey.setUserId(userId);
        idemKey.setExpiresAt(System.currentTimeMillis() + TTL_24_HOURS);

        idempotencyKeyRepository.save(idemKey);
        log.info("Idempotency key stored: {}", idempotencyKey);
    }

    @Override
    @Transactional
    public void cleanupExpiredKeys() {
        // In production, implement batch deletion of expired keys
        log.debug("Cleaning up expired idempotency keys");
    }

    @Override
    public String generateRequestHash(TransferRequestDto request) {
        try {
            // Generate hash from: sourceAccountId | destinationAccountId | amount
            String data = request.getSourceAccountId() + "|" +
                         request.getDestinationAccountId() + "|" +
                         request.getAmount();
            String hash = GeneratorUtil.hashRequest(data);
            log.debug("Generated request hash: {} for sourceAccount: {}, destAccount: {}, amount: {}",
                hash, request.getSourceAccountId(), request.getDestinationAccountId(), request.getAmount());
            return hash;
        } catch (Exception e) {
            log.error("Failed to generate request hash: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate request hash", e);
        }
    }

    @Override
    public <T> T getAndDeserializeResponse(String idempotencyKey, Class<T> responseType) {
        try {
            // Get cached response JSON
            String cachedResponseJson = getIdempotentResponse(idempotencyKey);
            if (cachedResponseJson == null || cachedResponseJson.isEmpty()) {
                log.warn("No cached response found for idempotency key: {}", idempotencyKey);
                return null;
            }

            // Deserialize JSON to response object
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            T deserializedResponse = mapper.readValue(cachedResponseJson, responseType);

            log.debug("Successfully deserialized cached response for idempotency key: {}", idempotencyKey);
            return deserializedResponse;
        } catch (Exception e) {
            log.error("Failed to deserialize cached response for idempotency key: {}: {}", idempotencyKey, e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize cached response", e);
        }
    }

    @Override
    public void serializeAndStoreResponse(String idempotencyKey, TransferRequestDto request, Object response, Integer statusCode, Long userId) {
        try {
            // Serialize response object to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String responseJson = mapper.writeValueAsString(response);

            // Generate hash from the request
            String hash = generateRequestHash(request);

            // Store in idempotency cache
            storeIdempotentResponse(idempotencyKey, hash, statusCode, responseJson, userId);

            log.debug("Serialized and stored idempotency response for key: {} with status code: {}", idempotencyKey, statusCode);
        } catch (Exception e) {
            log.error("Failed to serialize and store idempotency response for key: {}: {}", idempotencyKey, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize and store idempotency response", e);
        }
    }
}

