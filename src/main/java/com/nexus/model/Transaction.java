package com.nexus.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Transaction {
    private Long        id;
    private String      type;        // "INCOME" | "EXPENSE"
    private BigDecimal  amount;
    private String      currency;    // "UZS" | "USD"
    private String      category;
    private String      description;
    private LocalDate   txnDate;
    private LocalDateTime createdAt;
}
