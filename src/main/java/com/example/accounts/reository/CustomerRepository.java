package com.example.accounts.reository;

import com.example.accounts.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByKycId(String kycId);

    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate")
    Page<Customer> findCustomersByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    boolean existsByEmail(String email);

    /**
     * Find all customers assigned to a specific banker
     */
    Page<Customer> findByAssignedBankerBankerId(Long bankerId, Pageable pageable);

    /**
     * Find all customers assigned to a banker
     */
    long countByAssignedBankerBankerId(Long bankerId);

    /**
     * Find customers needing re-encryption (older than 10 days or with lower encryption version)
     */
    @Query(value = "SELECT c.* FROM customers c WHERE " +
           "c.encryption_version < ?1 OR " +
           "(c.last_encrypted_on IS NOT NULL AND c.last_encrypted_on < DATE_SUB(NOW(), INTERVAL 10 DAY) * 1000)",
           nativeQuery = true)
    List<Customer> findCustomersNeedingReEncryption(Integer currentVersion);
}

