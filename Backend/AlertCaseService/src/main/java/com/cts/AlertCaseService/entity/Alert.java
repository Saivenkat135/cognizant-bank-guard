package com.cts.AlertCaseService.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;
    private String severity;
    private LocalDateTime createdAt;
    private String decisionStatus;              // "flagged" or "terminated"
    private Double riskScore;                   // Risk score from Gemini
    @Column(length = 3000)
    private String reason;
    // Reason from Gemini (can be very long)
    private Long customerId;                    // Customer ID
}