package com.example.accounts.reository;

import com.example.accounts.entity.Account;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerCustomerId(Long customerId);

    Page<Account> findByCustomerCustomerId(Long customerId, Pageable pageable);

    Page<Account> findByAccountType(Account.AccountType accountType, Pageable pageable);

    List<Account> findByAccountType(Account.AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.customer.customerId = :customerId AND a.accountStatus = 'ACTIVE'")
    List<Account> findActiveAccountsByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT a FROM Account a WHERE a.createdAt >= :startDate AND a.createdAt <= :endDate")
    Page<Account> findAccountsByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);

    boolean existsByAccountNumber(String accountNumber);

    @Transactional
    @Modifying
    void deleteByCustomerCustomerId(Long customerId);

    /**
     * Find all accounts assigned to a specific banker
     */
    Page<Account> findByAssignedBankerBankerId(Long bankerId, Pageable pageable);

    /**
     * Find all accounts assigned to a banker
     */
    long countByAssignedBankerBankerId(Long bankerId);

    /**
     * Find accounts needing re-encryption (older than 10 days or with lower encryption version)
     */
    @Query(value = "SELECT a.* FROM accounts a WHERE a.encryption_version < ?1 OR " +
           "(a.last_encrypted_on IS NOT NULL AND a.last_encrypted_on < DATE_SUB(NOW(), INTERVAL 10 DAY) * 1000)",
           nativeQuery = true)
    List<Account> findAccountsNeedingReEncryption(Integer currentVersion);
}



