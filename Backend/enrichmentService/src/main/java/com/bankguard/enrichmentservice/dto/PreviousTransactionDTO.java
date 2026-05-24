package com.bankguard.enrichmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviousTransactionDTO {
    private Double amount;
    private String location;              // Combined location field
    private String ipAddress;
    private LocalDateTime time;
    private Double riskScore;
    private Long customerId;
}
