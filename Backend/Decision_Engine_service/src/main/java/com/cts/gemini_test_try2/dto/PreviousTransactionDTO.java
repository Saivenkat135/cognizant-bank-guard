package com.cts.gemini_test_try2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviousTransactionDTO {
    private Long transactionId;
    private Double amount;
    private String location;
    private String ipAddress;
    private LocalDateTime time;
    private Double riskScore;
    private Long customerId;
}
