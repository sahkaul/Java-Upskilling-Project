package com.example.accounts.reository;

import com.example.accounts.entity.Banker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankerRepository extends JpaRepository<Banker, Long> {

    /**
     * Find banker by userId
     */
    Optional<Banker> findByUserId(Long userId);


    /**
     * Find all active bankers with pagination
     */
    Page<Banker> findByIsActiveTrue(Pageable pageable);


    /**
     * Find bankers by branch code with pagination
     */
    Page<Banker> findByBranchCodeAndIsActiveTrue(String branchCode, Pageable pageable);

    /**
     * Find bankers by portfolio with pagination
     */
    Page<Banker> findByPortfolioAndIsActiveTrue(String portfolio, Pageable pageable);


    /**
     * Check if user ID already exists
     */
    boolean existsByUserId(Long userId);

}

