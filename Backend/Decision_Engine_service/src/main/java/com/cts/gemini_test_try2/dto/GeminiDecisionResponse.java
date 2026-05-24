package com.cts.gemini_test_try2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiDecisionResponse {
    @JsonProperty("riskScore")
    private Double riskScore;
    
    @JsonProperty("decision")
    private String decision; // "Genuine", "flagged", "terminated"
    
    @JsonProperty("reason")
    private String reason; // Basis for the decision
}
