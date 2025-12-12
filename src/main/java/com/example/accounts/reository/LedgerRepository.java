package com.example.accounts.reository;

import com.example.accounts.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT l FROM LedgerEntry l WHERE l.account.accountId = :accountId ORDER BY l.createdAt DESC")
    Page<LedgerEntry> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT l FROM LedgerEntry l WHERE l.ledgerTxnId = :ledgerTxnId")
    List<LedgerEntry> findByLedgerTxnId(@Param("ledgerTxnId") String ledgerTxnId);

    @Query("SELECT l FROM LedgerEntry l WHERE l.account.accountId = :accountId AND l.createdAt >= :startDate AND l.createdAt <= :endDate")
    List<LedgerEntry> findLedgerByAccountAndDateRange(@Param("accountId") Long accountId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.entryType = 'CREDIT' THEN l.amount ELSE -l.amount END), 0) FROM LedgerEntry l WHERE l.account.accountId = :accountId")
    BigDecimal calculateAccountBalance(@Param("accountId") Long accountId);
}

