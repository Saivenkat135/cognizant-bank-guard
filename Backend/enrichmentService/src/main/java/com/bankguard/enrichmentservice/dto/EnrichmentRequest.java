package com.bankguard.enrichmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentRequest {
    private TransactionDTO currentTransaction;
    private CustomerDTO customer;
    private List<TransactionDTO> previousTransactions;
}
