package com.example.accounts.reository;

import com.example.accounts.entity.RateLimitEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateLimitRepository extends JpaRepository<RateLimitEntry, Long> {

    Optional<RateLimitEntry> findByUserIdAndEndpointAndWindowStart(Long userId, String endpoint, Long windowStart);
}

