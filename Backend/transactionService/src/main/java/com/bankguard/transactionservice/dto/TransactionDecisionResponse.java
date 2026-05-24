package com.bankguard.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDecisionResponse {
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
    private String decision;            // "genuine", "flagged", "terminated"
    private String reason;
    private boolean alertSent;  // true if alert was sent to AlertCaseService
}

