package com.bankguard.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentRequestDTO {
    private TransactionEnrichmentDTO currentTransaction;
    private CustomerEnrichmentDTO customer;
    private List<TransactionEnrichmentDTO> previousTransactions;
}
