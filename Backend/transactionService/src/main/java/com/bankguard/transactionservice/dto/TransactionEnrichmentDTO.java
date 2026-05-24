package com.bankguard.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEnrichmentDTO {
    private Long transactionId;
    private Double amount;
    private String city;
    private String state;
    private String ipAddress;
    private LocalDateTime time;
    private Double riskScore;
    private String receiverAccountNumber;
    private Long customerId;
}
