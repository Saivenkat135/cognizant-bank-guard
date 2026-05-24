package com.bankguard.transactionservice.service;

import com.bankguard.transactionservice.dto.CustomerEnrichmentDTO;
import com.bankguard.transactionservice.dto.EnrichmentRequestDTO;
import com.bankguard.transactionservice.dto.TransactionDecisionResponse;
import com.bankguard.transactionservice.dto.TransactionEnrichmentDTO;
import com.bankguard.transactionservice.entity.Customer;
import com.bankguard.transactionservice.entity.Transaction;
import com.bankguard.transactionservice.exception.ReceiverAccountNotFoundException;
import com.bankguard.transactionservice.exception.TransactionProcessingException;
import com.bankguard.transactionservice.repository.CustomerRepository;
import com.bankguard.transactionservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionEnrichmentIntegrationService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private final WebClient webClient;

    @Value("${enrichment.service.url:http://localhost:8010}")
    private String enrichmentServiceUrl;

    public TransactionEnrichmentIntegrationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Send transaction data to Enrichment Service
     * Includes: current transaction + customer profile + last 5 previous transactions
     */
    public TransactionDecisionResponse enrichTransactionWithService(Transaction transaction, Customer customer) {
        try {
            // Create current transaction DTO
            TransactionEnrichmentDTO currentTransactionDTO = new TransactionEnrichmentDTO();
            currentTransactionDTO.setTransactionId(transaction.getTransactionId());
            currentTransactionDTO.setAmount(transaction.getAmount());
            currentTransactionDTO.setCity(transaction.getCity());
            currentTransactionDTO.setState(transaction.getState());
            currentTransactionDTO.setIpAddress(transaction.getIpAddress());
            currentTransactionDTO.setTime(transaction.getTime());
            currentTransactionDTO.setRiskScore(transaction.getRiskScore());
            currentTransactionDTO.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
            currentTransactionDTO.setCustomerId(transaction.getCustomerId());

            // Create customer DTO
            CustomerEnrichmentDTO customerDTO = new CustomerEnrichmentDTO();
            if (customer != null) {
                customerDTO.setCustomerId(customer.getCustomerId());
                customerDTO.setBankName(customer.getBankName());
                customerDTO.setBalance(customer.getBalance());
                customerDTO.setAccountType(customer.getAccountType());
                customerDTO.setName(customer.getName());
                customerDTO.setEmail(customer.getEmail());
                customerDTO.setAccountNo(customer.getAccountNo());
            }

            // Get previous transactions (max 5, excluding current transaction)
            List<TransactionEnrichmentDTO> previousTransactionsDTO = getLastPreviousTransactions(
                    transaction.getCustomerId(), 
                    transaction.getTransactionId(), 
                    5
            );

            // Create enrichment request
            EnrichmentRequestDTO enrichmentRequest = new EnrichmentRequestDTO();
            enrichmentRequest.setCurrentTransaction(currentTransactionDTO);
            enrichmentRequest.setCustomer(customerDTO);
            enrichmentRequest.setPreviousTransactions(previousTransactionsDTO);

            // Send to Enrichment Service with Decision and Alert routing (includes Gemini analysis and AlertCase routing)
            String enrichmentUrl = enrichmentServiceUrl + "/api/enrich/transaction/with-decision-and-alert";
            TransactionDecisionResponse enrichedResponse = webClient.post()
                    .uri(enrichmentUrl)
                    .bodyValue(enrichmentRequest)
                    .retrieve()
                    .bodyToMono(TransactionDecisionResponse.class)
                    .block();

            return enrichedResponse;

        } catch (Exception e) {
            throw new RuntimeException("Error enriching transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Get last N previous transactions for a customer (excluding current transaction)
     */
    private List<TransactionEnrichmentDTO> getLastPreviousTransactions(
            Long customerId, 
            Long excludeTransactionId, 
            int limit) {
        
        List<Transaction> allTransactions = transactionRepository.findByCustomerId(customerId);
        
        return allTransactions.stream()
                .filter(t -> !t.getTransactionId().equals(excludeTransactionId))
                .sorted((t1, t2) -> {
                    if (t2.getTime() == null || t1.getTime() == null) {
                        return 0;
                    }
                    return t2.getTime().compareTo(t1.getTime());
                })
                .limit(limit)
                .map(this::convertTransactionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert Transaction entity to TransactionEnrichmentDTO
     */
    private TransactionEnrichmentDTO convertTransactionToDTO(Transaction transaction) {
        TransactionEnrichmentDTO dto = new TransactionEnrichmentDTO();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setAmount(transaction.getAmount());
        dto.setCity(transaction.getCity());
        dto.setState(transaction.getState());
        dto.setIpAddress(transaction.getIpAddress());
        dto.setTime(transaction.getTime());
        dto.setRiskScore(transaction.getRiskScore());
        dto.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
        dto.setCustomerId(transaction.getCustomerId());
        return dto;
    }


    /**
     * Process enrichment response and handle account balance updates
     * If status is "genuine", credit receiver's account and debit sender's account
     * Throws exception if receiver's account number is not found
     *
     * @param enrichedResponse Response from enrichment service (as Map or Object)
     * @param transaction The transaction being processed
     * @param senderCustomer The customer sending the transaction (sender)
     * @return Updated transaction object or error message
     */
    public Map<String, Object> processEnrichmentResponse(
            Object enrichedResponse, 
            Transaction transaction, 
            Customer senderCustomer) {
        
        try {
            // Convert response to Map for easier access
            Map<String, Object> responseMap = null;
            
            if (enrichedResponse instanceof Map) {
                responseMap = (Map<String, Object>) enrichedResponse;
            } else {
                // If it's not a Map, try to cast or handle error
                throw new TransactionProcessingException(
                    "Invalid enrichment response format: expected Map but got " + 
                    (enrichedResponse != null ? enrichedResponse.getClass().getName() : "null")
                );
            }

            // Extract status from response (case-insensitive)
            String status = responseMap.get("status") != null ? 
                    responseMap.get("status").toString().toLowerCase() : "";

            // Process balance updates for "genuine" and "flagged" transactions
            // Only skip for "terminated" transactions
            if (!"terminated".equalsIgnoreCase(status)) {
                
                // Step 1: Validate that receiver's account exists
                String receiverAccountNumber = transaction.getReceiverAccountNumber();
                
                if (receiverAccountNumber == null || receiverAccountNumber.trim().isEmpty()) {
                    throw new ReceiverAccountNotFoundException(
                        "Receiver account number is null or empty",
                        "UNKNOWN"
                    );
                }

                Customer receiverCustomer = customerRepository.findByAccountNo(receiverAccountNumber);
                
                if (receiverCustomer == null) {
                    throw new ReceiverAccountNotFoundException(receiverAccountNumber);
                }

                // Step 2: Process account updates
                Double transactionAmount = transaction.getAmount();
                
                if (transactionAmount == null || transactionAmount <= 0) {
                    throw new TransactionProcessingException(
                        "Invalid transaction amount: " + transactionAmount,
                        "INVALID_AMOUNT"
                    );
                }

                // Credit receiver's account
                Double receiverCurrentBalance = receiverCustomer.getBalance();
                receiverCustomer.setBalance(receiverCurrentBalance + transactionAmount);

                // Debit sender's account
                Double senderCurrentBalance = senderCustomer.getBalance();
                
                if (senderCurrentBalance < transactionAmount) {
                    throw new TransactionProcessingException(
                        "Insufficient balance in sender's account. Current: " + senderCurrentBalance + 
                        ", Required: " + transactionAmount,
                        "INSUFFICIENT_BALANCE"
                    );
                }
                
                senderCustomer.setBalance(senderCurrentBalance - transactionAmount);

                // Step 3: Save both customers with updated balances
                customerRepository.save(senderCustomer);
                customerRepository.save(receiverCustomer);

                // Return success response with updated balances
                return Map.of(
                    "status", "SUCCESS",
                    "message", "Transaction processed successfully",
                    "transactionStatus", status,
                    "senderNewBalance", senderCustomer.getBalance(),
                    "receiverNewBalance", receiverCustomer.getBalance(),
                    "enrichmentResponse", enrichedResponse
                );

            } else {
                // If status is "terminated", decline the transaction without balance updates
                return Map.of(
                    "status", "DECLINED",
                    "message", "Transaction terminated by enrichment service. No balance updates performed.",
                    "transactionStatus", status,
                    "enrichmentResponse", enrichedResponse
                );
            }

        } catch (ReceiverAccountNotFoundException e) {
            throw e; // Re-throw custom exceptions
        } catch (TransactionProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new TransactionProcessingException(
                "Error processing enrichment response: " + e.getMessage(),
                e
            );
        }
    }
}
