package com.bankguard.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEnrichmentDTO {
    private Long customerId;
    private String bankName;
    private Double balance;
    private String accountType;
    private String name;
    private String email;
    private String accountNo;
}
