package com.example.accounts.reository;

import com.example.accounts.entity.TransferVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferVersionRepository extends JpaRepository<TransferVersion, Long> {

    @Query("SELECT tv FROM TransferVersion tv WHERE tv.transfer.transferId = :transferId ORDER BY tv.versionNumber DESC")
    List<TransferVersion> findByTransferId(@Param("transferId") Long transferId);

    /**
     * Find all versions for a transfer sorted by version number (most recent first)
     */
    @Query("SELECT tv FROM TransferVersion tv WHERE tv.transfer.transferId = :transferId ORDER BY tv.versionNumber DESC")
    List<TransferVersion> findByTransfer_TransferId(@Param("transferId") Long transferId);

    @Query("SELECT tv FROM TransferVersion tv WHERE tv.transfer.transferId = :transferId ORDER BY tv.versionNumber DESC")
    Page<TransferVersion> findByTransferIdPaginated(@Param("transferId") Long transferId, Pageable pageable);

    TransferVersion findByTransferTransferIdAndVersionNumber(Long transferId, Long versionNumber);

    /**
     * Get the maximum version number for a transfer
     */
    @Query("SELECT MAX(tv.versionNumber) FROM TransferVersion tv WHERE tv.transfer.transferId = :transferId")
    Long getMaxVersionNumber(@Param("transferId") Long transferId);
}

