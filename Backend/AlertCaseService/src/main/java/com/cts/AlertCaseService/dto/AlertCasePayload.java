package com.cts.AlertCaseService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertCasePayload {
    private String reason;
    private String decisionStatus;              // "flagged" or "terminated"
    private Double geminiRiskScore;             // Risk score from Gemini

    private String customerName;
    private Long customerId;// Customer name
    private String city;
    private String state;
    private String customerEmail;
    private String customerAccountNo;
    private Double customerBalance;

    private Double amount;                      // Transaction amount
    private LocalDateTime time;                   // Transaction amount
}
