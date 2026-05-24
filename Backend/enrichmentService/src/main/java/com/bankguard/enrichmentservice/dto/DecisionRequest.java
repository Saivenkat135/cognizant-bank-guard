package com.bankguard.enrichmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequest {
    // Current Transaction Details
    private Double amount;
    private String location;              // Combined location field (city, region, or full address)
    private LocalDateTime time;
    private Double riskScore;
    
    // Customer Profile
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerAccountNo;
    private Double customerBalance;
    
    // Previous 5 Transactions
    private List<com.bankguard.enrichmentservice.dto.PreviousTransactionDTO> previousTransactions;
}
