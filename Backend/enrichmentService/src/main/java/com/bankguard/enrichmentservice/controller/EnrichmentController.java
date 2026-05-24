package com.bankguard.enrichmentservice.controller;

import com.bankguard.enrichmentservice.dto.*;
import com.bankguard.enrichmentservice.service.EnrichmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static reactor.netty.http.HttpConnectionLiveness.log;

@RestController
@RequestMapping("/api/enrich")
public class EnrichmentController {

    @Autowired
    private EnrichmentService enrichmentService;

@PostMapping("/transaction/with-decision-and-alert")
public ResponseEntity<TransactionDecisionResponse> enrichTransactionWithDecisionAndAlert(@RequestBody EnrichmentRequest request) {
    try {
        // Validate input
        if (request == null || request.getCurrentTransaction() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        TransactionDecisionResponse response = enrichmentService.enrichAndDecideWithConditionalAlert(request);
        return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
        log.warn("Invalid request payload", e);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
        // ADD THIS LOG: This will print the EXACT reason for the 500 error in your terminal
        log.error("CRITICAL: Internal Server Error during transaction enrichment", e);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


    @PostMapping("/validate/amount")
    public ResponseEntity<Boolean> validateAmount(@RequestParam Double amount) {
        try {
            boolean isValid = enrichmentService.validateTransactionAmount(amount);
            return new ResponseEntity<>(isValid, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/validate/balance")
    public ResponseEntity<Boolean> validateBalance(
            @RequestParam Double customerBalance,
            @RequestParam Double transactionAmount) {
        try {
            boolean isValid = enrichmentService.validateSufficientBalance(customerBalance, transactionAmount);
            return new ResponseEntity<>(isValid, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/validate/ip")
    public ResponseEntity<Boolean> validateIpAddress(@RequestParam String ipAddress) {
        try {
            boolean isValid = enrichmentService.validateIpAddress(ipAddress);
            return new ResponseEntity<>(isValid, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
