package com.example.accounts.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class StatementDto implements Serializable {

    private Long accountId;
    private String accountNumber;
    private String customerName;
    private String accountType;
    private String accountStatus;
    private String currency;

    private Integer month;
    private Integer year;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private BigDecimal openingBalance;
    private BigDecimal closingBalance;

    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private BigDecimal totalInterest;
    private BigDecimal totalFees;

    private Long transactionCount;
    private LocalDateTime generatedAt;

    private List<StatementLineItemDto> lineItems;
}

