package com.bankguard.transactionservice.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankguard.transactionservice.dto.TransactionCreationRequest;
import com.bankguard.transactionservice.dto.TransactionDecisionResponse;
import com.bankguard.transactionservice.entity.Customer;
import com.bankguard.transactionservice.entity.Transaction;
import com.bankguard.transactionservice.exception.ReceiverAccountNotFoundException;
import com.bankguard.transactionservice.exception.TransactionProcessingException;
import com.bankguard.transactionservice.repository.CustomerRepository;
import com.bankguard.transactionservice.repository.TransactionRepository;
import com.bankguard.transactionservice.service.TransactionEnrichmentIntegrationService;
import com.bankguard.transactionservice.service.TransactionService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionEnrichmentIntegrationService enrichmentIntegrationService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody TransactionCreationRequest request) {
        log.info("=== TRANSACTION START === Sender: {}, Amount: {}, Receiver: {}", 
                 request.getCustomerId(), request.getAmount(), request.getReceiverAccountNumber());
        
        try {
            // ====== STEP 1: VALIDATE ALL INPUT BEFORE ANY DATABASE CHANGES ======
            
            // 1.1: Validate sender customer exists
            Customer senderCustomer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Sender customer not found with ID: " + request.getCustomerId()));
            log.debug("✓ Sender found: {}", senderCustomer.getCustomerId());

            // 1.2: Validate transaction amount is valid
            if (request.getAmount() == null || request.getAmount() <= 0) {
                String msg = "Transaction amount must be positive: " + request.getAmount();
                log.error("✗ {}", msg);
                throw new IllegalArgumentException(msg);
            }
            log.debug("✓ Amount validated: {}", request.getAmount());

            // 1.3: CRITICAL - Check SENDER HAS SUFFICIENT BALANCE BEFORE ANY UPDATE
            if (senderCustomer.getBalance() < request.getAmount()) {
                String msg = String.format(
                    "Insufficient balance in sender's account. Current: %.2f, Required: %.2f",
                    senderCustomer.getBalance(), request.getAmount()
                );
                log.error("✗ {}", msg);
                throw new TransactionProcessingException(msg, "INSUFFICIENT_BALANCE");
            }
            log.debug("✓ Sender has sufficient balance. Current: {}", senderCustomer.getBalance());

            // 1.4: CRITICAL - Validate RECEIVER EXISTS BEFORE ANY UPDATE
            Customer receiverCustomer = customerRepository.findByAccountNo(request.getReceiverAccountNumber());
            if (receiverCustomer == null) {
                String msg = "Receiver account not found: " + request.getReceiverAccountNumber();
                log.error("✗ {}", msg);
                throw new ReceiverAccountNotFoundException(request.getReceiverAccountNumber());
            }
            log.debug("✓ Receiver found: {} (Account: {})", receiverCustomer.getCustomerId(), receiverCustomer.getAccountNo());

            // ====== STEP 2: CREATE TRANSACTION OBJECT ======
            log.debug("Step 2: Creating transaction object...");
            
            Transaction transaction = new Transaction();
            transaction.setAmount(request.getAmount());
            transaction.setCity(request.getCity());
            transaction.setState(request.getState());
            transaction.setIpAddress(request.getIpAddress());
            transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
            transaction.setCustomerId(request.getCustomerId());
            transaction.setTime(LocalDateTime.now());
            // Set default risk score to 0.0 if null
            transaction.setRiskScore(senderCustomer.getRiskScore() == null ? 0.0 : senderCustomer.getRiskScore());
            log.debug("✓ Transaction object created");

            // ====== STEP 3: GET ENRICHMENT ANALYSIS ======
            log.debug("Step 3: Calling enrichment service...");
            
            TransactionDecisionResponse enrichedResponse = enrichmentIntegrationService.enrichTransactionWithService(transaction, senderCustomer);
            log.debug("✓ Enrichment completed with decision: {}", enrichedResponse.getDecision());
            
            // Update transaction with enriched data (risk score clamped to [0,100])
            transaction.setCity(enrichedResponse.getCity());
            transaction.setState(enrichedResponse.getState());
            transaction.setAmount(enrichedResponse.getAmount());
            transaction.setTime(enrichedResponse.getTime());
            transaction.setStatus(enrichedResponse.getDecision());
            transaction.setReason(enrichedResponse.getReason());
            transaction.setRiskScore(clampRisk(enrichedResponse.getRiskScore()));

            // ====== STEP 4: CALCULATE AVERAGE RISK SCORE (FIXED CALCULATION) ======
            log.debug("Step 4: Calculating updated risk score...");
            
            // FIXED: Calculate average of sender's own transactions, not global count
            List<Transaction> senderTransactions = transactionRepository.findByCustomerId(request.getCustomerId());
            
            Double enrichedRiskScore = clampRisk(enrichedResponse.getRiskScore());
            Double newRiskScore;

            if (senderTransactions.isEmpty()) {
                newRiskScore = enrichedRiskScore;
                log.debug("No previous transactions. New risk score: {}", newRiskScore);
            } else {
                double sumRiskScore = senderTransactions.stream()
                        .mapToDouble(t -> t.getRiskScore() == null ? 0.0 : t.getRiskScore())
                        .sum() + enrichedRiskScore;
                newRiskScore = sumRiskScore / (senderTransactions.size() + 1);
                log.debug("Previous transactions: {}. New risk score: {} (average of {})",
                         senderTransactions.size(), newRiskScore, senderTransactions.size() + 1);
            }

            newRiskScore = clampRisk(newRiskScore);
            senderCustomer.setRiskScore(newRiskScore);

            // ====== STEP 5: PROCESS BALANCE UPDATES (skipped when terminated) ======
            // When enrichment terminates the transaction we still persist the
            // attempt + the customer's new risk score, but balances stay put.
            boolean isTerminated = "terminated".equalsIgnoreCase(enrichedResponse.getDecision());

            Double newSenderBalance   = senderCustomer.getBalance();
            Double newReceiverBalance = receiverCustomer.getBalance();

            if (isTerminated) {
                log.info("Decision=terminated → skipping balance updates. Sender: {}, Receiver: {}",
                         newSenderBalance, newReceiverBalance);
            } else {
                log.debug("Step 5: Processing balance updates...");
                log.info("Balance Update - Sender before: {}, Receiver before: {}",
                         senderCustomer.getBalance(), receiverCustomer.getBalance());

                Double senderBalance = senderCustomer.getBalance() == null ? 0.0 : senderCustomer.getBalance();
                newSenderBalance = senderBalance - request.getAmount();
                senderCustomer.setBalance(newSenderBalance);
                log.debug("✓ Deducted {} from sender. New balance: {}", request.getAmount(), newSenderBalance);

                Double receiverBalance = receiverCustomer.getBalance() == null ? 0.0 : receiverCustomer.getBalance();
                newReceiverBalance = receiverBalance + request.getAmount();
                receiverCustomer.setBalance(newReceiverBalance);
                log.debug("✓ Credited {} to receiver. New balance: {}", request.getAmount(), newReceiverBalance);
            }

            // ====== STEP 6: SAVE ALL CHANGES ======
            log.debug("Step 6: Saving all changes to database...");

            // Sender row always saved — at minimum the updated risk score persists.
            customerRepository.save(senderCustomer);
            log.debug("✓ Sender customer saved");

            // Receiver row only changes when we actually moved money.
            if (!isTerminated) {
                customerRepository.save(receiverCustomer);
                log.debug("✓ Receiver customer saved");
            }

            Transaction savedTransaction = transactionService.saveTransaction(transaction);
            log.debug("✓ Transaction saved with ID: {}", savedTransaction.getTransactionId());

            // ====== STEP 7: RETURN SUCCESS RESPONSE ======
            log.info("✓ TRANSACTION SUCCESS === Transaction ID: {}, Amount: {}", 
                     savedTransaction.getTransactionId(), request.getAmount());
            
            return new ResponseEntity<>(
                    Map.of(
                            "status", "SUCCESS",
                            "transactionId", savedTransaction.getTransactionId(),
                            "transaction", savedTransaction,
                            "senderNewBalance", newSenderBalance,
                            "receiverNewBalance", newReceiverBalance,
                            "transactionStatus", enrichedResponse.getDecision(),
                            "riskScore", newRiskScore,
                            "timestamp", LocalDateTime.now()
                    ),
                    HttpStatus.CREATED
            );

        } catch (IllegalArgumentException e) {
            log.error("✗ Validation error: {}", e.getMessage());
            return new ResponseEntity<>(
                    Map.of(
                            "status", "ERROR",
                            "errorCode", "INVALID_REQUEST",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ),
                    HttpStatus.BAD_REQUEST
            );
        } catch (ReceiverAccountNotFoundException e) {
            log.error("✗ Receiver not found: {}", e.getMessage());
            return new ResponseEntity<>(
                    Map.of(
                            "status", "ERROR",
                            "errorCode", "RECEIVER_NOT_FOUND",
                            "message", e.getMessage(),
                            "receiverAccountNumber", e.getAccountNumber(),
                            "timestamp", LocalDateTime.now()
                    ),
                    HttpStatus.NOT_FOUND
            );
        } catch (TransactionProcessingException e) {
            log.error("✗ Transaction processing error: {} [{}]", e.getMessage(), e.getErrorCode());
            return new ResponseEntity<>(
                    Map.of(
                            "status", "ERROR",
                            "errorCode", e.getErrorCode(),
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("✗ UNEXPECTED ERROR: ", e);
            return new ResponseEntity<>(
                    Map.of(
                            "status", "ERROR",
                            "errorCode", "INTERNAL_ERROR",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }



    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction != null) {
            return new ResponseEntity<>(transaction, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable Long transactionId, @RequestBody Transaction transactionDetails) {
        Transaction updatedTransaction = transactionService.updateTransaction(transactionId, transactionDetails);
        if (updatedTransaction != null) {
            return new ResponseEntity<>(updatedTransaction, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<String> deleteTransaction(@PathVariable Long transactionId) {
        boolean deleted = transactionService.deleteTransaction(transactionId);
        if (deleted) {
            return new ResponseEntity<>("Transaction deleted successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Transaction not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Transaction>> getTransactionsByCustomerId(@PathVariable Long customerId) {
        List<Transaction> transactions = transactionService.getTransactionsByCustomerId(customerId);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @GetMapping("/receiver/{receiverAccountNumber}")
    public ResponseEntity<List<Transaction>> getTransactionsByReceiverAccount(@PathVariable String receiverAccountNumber) {
        List<Transaction> transactions = transactionService.getTransactionsByReceiverAccount(receiverAccountNumber);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    /** Risk score must always be in [0,100]. Null → 0. */
    private static Double clampRisk(Double v) {
        if (v == null) return 0.0;
        if (v < 0.0)   return 0.0;
        if (v > 100.0) return 100.0;
        return v;
    }
}