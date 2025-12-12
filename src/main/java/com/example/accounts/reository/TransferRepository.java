package com.example.accounts.reository;

import com.example.accounts.entity.Transfer;
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
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transfer t WHERE t.sourceAccount.accountId = :accountId OR t.destinationAccount.accountId = :accountId")
    Page<Transfer> findTransfersByAccount(@Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE t.sourceAccount.accountId = :accountId")
    Page<Transfer> findOutgoingTransfers(@Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE t.destinationAccount.accountId = :accountId")
    Page<Transfer> findIncomingTransfers(@Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE t.transferStatus = :status")
    Page<Transfer> findByTransferStatus(@Param("status") Transfer.TransferStatus status, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE t.createdAt >= :startDate AND t.createdAt <= :endDate")
    Page<Transfer> findTransfersByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE t.sourceAccount.accountId = :accountId AND t.createdAt >= :date")
    List<Transfer> findTransfersForDailyLimit(@Param("accountId") Long accountId, @Param("date") LocalDateTime date);
}

