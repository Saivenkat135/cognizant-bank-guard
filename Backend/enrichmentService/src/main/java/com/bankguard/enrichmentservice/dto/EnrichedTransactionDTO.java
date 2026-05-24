package com.bankguard.enrichmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedTransactionDTO {
    private Double amount;
    private String city;
    private String state;
    private LocalDateTime time;
    private Double riskScore;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerAccountNo;
    private Double customerBalance;
    private List<TransactionDTO> previousTransactions;
}
