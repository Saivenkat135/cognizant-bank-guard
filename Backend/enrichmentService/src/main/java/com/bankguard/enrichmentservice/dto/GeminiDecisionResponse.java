package com.bankguard.enrichmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiDecisionResponse {
    private Double riskScore;          // Risk score from Gemini analysis
    private String decision;            // "genuine", "flagged", "terminated"
    private String reason;              // Reasoning for the decision
}
