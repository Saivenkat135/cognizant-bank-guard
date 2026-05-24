package com.cts.AlertCaseService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportingRequest {
    private Long caseId;
    private String status;                      // Case status (OPEN, UNDER_INVESTIGATION, RESOLVED, etc.)
    private double riskScore;                   // Risk score from Gemini
    private String reason;                      // Reason for the fraud alert (from Gemini)
    private String geminiDecision;              // "flagged", "terminated"
    private Double amount;                      // Transaction amount

    private String city;
    private String state;
    private String customerEmail;
    private Long customerId;
    private String customerAccountNo;
    private Double customerBalance;
    private String customerName;                // Customer name

    private LocalDateTime time;
}
