package com.cts.AlertCaseService.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class CaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long caseId;
    private Long alertId;
    private String caseStatus;
    @Column(length = 3000)
    private String reason;

    private LocalDateTime createdAt;
    private Double riskScore;                    // Gemini risk score
    private String decisionStatus;               // "flagged", "terminated", "genuine"
    private Double amount;                       // Transaction amount
    private String customerName;                 // Customer name from enrichment
    private Double customerBalance;              // Customer balance at time of transaction

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CaseCustomer customer;
}
