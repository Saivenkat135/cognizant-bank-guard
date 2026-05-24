package com.cts.gemini_test_try2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in decision_db.decision_rules — a rule the Risk Manager wants
 * Gemini to consider while scoring transactions.
 */
@Entity
@Table(name = "decision_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_text", nullable = false, length = 1000)
    private String text;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;
}
