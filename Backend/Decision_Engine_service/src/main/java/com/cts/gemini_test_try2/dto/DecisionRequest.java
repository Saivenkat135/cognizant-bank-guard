package com.cts.gemini_test_try2.dto;

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
    private Long transactionId;
    private Double amount;
    private String location;
    private LocalDateTime time;
    private Double riskScore;
    
    // Customer Profile
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerAccountNo;
    private Double customerBalance;
    
    // Previous 5 Transactions
    private List<PreviousTransactionDTO> previousTransactions;
}
