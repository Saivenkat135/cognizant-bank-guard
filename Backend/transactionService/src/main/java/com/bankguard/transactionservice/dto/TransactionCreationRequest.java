package com.bankguard.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreationRequest {
    private Double amount;
    private String city;
    private String state;
    private String ipAddress;
    private String receiverAccountNumber;
    private Long customerId;
}
