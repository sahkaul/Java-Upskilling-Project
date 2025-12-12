package com.example.accounts.reository;

import com.example.accounts.entity.TransferHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransferHoldRepository extends JpaRepository<TransferHold, Long> {

    List<TransferHold> findByAccountAccountIdAndReleasedFalse(Long accountId);

    List<TransferHold> findByTransferTransferId(Long transferId);

    @Query("SELECT COALESCE(SUM(th.holdAmount), 0) FROM TransferHold th WHERE th.account.accountId = :accountId AND th.released = false")
    BigDecimal calculateTotalHolds(@Param("accountId") Long accountId);
}

