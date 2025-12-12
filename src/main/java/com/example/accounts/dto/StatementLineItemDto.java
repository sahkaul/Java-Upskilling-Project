package com.example.accounts.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class StatementLineItemDto implements Serializable {

    private LocalDateTime transactionDate;
    private String entryType;  // DEBIT or CREDIT
    private BigDecimal amount;
    private String description;
    private String referenceType;  // TRANSFER, INTEREST, FEE, etc.
    private BigDecimal runningBalance;
}

