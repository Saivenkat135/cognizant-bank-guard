package com.cts.gemini_test_try2.Service;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cts.gemini_test_try2.dto.DecisionRequest;
import com.cts.gemini_test_try2.dto.GeminiDecisionResponse;
import com.cts.gemini_test_try2.dto.PreviousTransactionDTO;
import com.cts.gemini_test_try2.entity.DecisionRule;
import com.cts.gemini_test_try2.repository.DecisionRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Autowired
    private Client client;

    @Autowired
    private DecisionRuleRepository ruleRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analyzes a transaction with Gemini AI to determine fraud risk
     */
    public GeminiDecisionResponse analyzeTransactionWithGemini(DecisionRequest request) throws HttpException, IOException {
        // Extract location from topmost previous transaction
        String previousLocation = extractPreviousLocation(request.getPreviousTransactions());

        // Calculate average amount from previous transactions
        Double averageAmount = calculateAverageAmount(request.getPreviousTransactions());

        // Apply validations and update risk score
        Double updatedRiskScore = applyValidations(request.getRiskScore(), request.getAmount(),
                                                   averageAmount, request.getLocation(), previousLocation);

        // Pull the Risk Manager's rules from decision_db and dump them to the
        // console — quick visual confirmation that Gemini is seeing them.
        List<DecisionRule> rules = ruleRepo.findAll();
        System.out.println("[DecisionRules] Loading " + rules.size() + " rule(s) for Gemini prompt:");
        for (DecisionRule r : rules) {
            System.out.println("  - #" + r.getId()
                    + " (riskScore=" + r.getRiskScore() + ") "
                    + r.getText());
        }

        // Build the prompt for Gemini
        String prompt = buildAnalysisPrompt(request, averageAmount, previousLocation, updatedRiskScore, rules);

        // Get response from Gemini
        String geminiResponse = askGemini(prompt);

        // Parse and return the response
        return parseGeminiResponse(geminiResponse);
    }

    /**
     * Extract location (state name) from the topmost (first) previous transaction
     */
    private String extractPreviousLocation(List<PreviousTransactionDTO> previousTransactions) {
        if (previousTransactions != null && !previousTransactions.isEmpty()) {
            String location = previousTransactions.get(0).getLocation();
            // Extract state name (assuming format like "City, State")
            return extractStateName(location);
        }
        return "UNKNOWN";
    }

    /**
     * Extract state name from location string
     */
    private String extractStateName(String location) {
        if (location == null || location.isEmpty()) {
            return "UNKNOWN";
        }
        String[] parts = location.split(",");
        if (parts.length > 1) {
            return parts[parts.length - 1].trim();
        }
        return location.trim();
    }

    /**
     * Calculate average amount from previous transactions
     */
    private Double calculateAverageAmount(List<PreviousTransactionDTO> previousTransactions) {
        if (previousTransactions == null || previousTransactions.isEmpty()) {
            return 0.0;
        }
        
        Double sum = previousTransactions.stream()
                .mapToDouble(PreviousTransactionDTO::getAmount)
                .sum();
        
        return sum / previousTransactions.size();
    }

    /**
     * Apply validation rules to update risk score
     * Validation 1: If average amount >= current amount + 5% of average → increment risk by 10
     * Validation 2: If previous location (state) matches current location (state) → increment risk by 20
     */
    private Double applyValidations(Double currentRiskScore, Double currentAmount, 
                                    Double averageAmount, String currentLocation, String previousLocation) {
        Double updatedScore = currentRiskScore;
        
        // Validation 1: Check if average amount is suspicious relative to current transaction
        Double threshold = averageAmount + (averageAmount * 0.05);
        if (currentAmount <= threshold) {
            // Current amount is within acceptable range, but lower than average - could indicate fraud
            updatedScore += 10;
        }
        
        // Validation 2: Check if location matches previous location
        String currentState = extractStateName(currentLocation);
        if (currentState.equalsIgnoreCase(previousLocation)) {
            // Same location as previous transactions - increment risk
            updatedScore += 20;
        }
        
        return updatedScore;
    }

    /**
     * Build comprehensive prompt for Gemini with transaction analysis
     */
    private String buildAnalysisPrompt(DecisionRequest request, Double averageAmount,
                                      String previousLocation, Double updatedRiskScore,
                                      List<DecisionRule> rules) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a fraud detection AI expert. Analyze the following transaction for fraud risk.\n\n");

        // RiskManager-authored rules from the database. When present we want
        // Gemini to weigh each match into the final risk score.
        if (rules != null && !rules.isEmpty()) {
            prompt.append("RISK MANAGER RULES (apply each if it fits — add the rule's risk score to the total when it triggers):\n");
            for (DecisionRule r : rules) {
                prompt.append("- (").append(r.getRiskScore()).append(") ")
                      .append(r.getText()).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("TRANSACTION DETAILS:\n");
        prompt.append("- Transaction ID: ").append(request.getTransactionId()).append("\n");
        prompt.append("- Amount: $").append(String.format("%.2f", request.getAmount())).append("\n");
        prompt.append("- Location: ").append(request.getLocation()).append("\n");
        prompt.append("- Time: ").append(request.getTime()).append("\n");
        
        prompt.append("\nCUSTOMER PROFILE:\n");
        prompt.append("- Customer ID: ").append(request.getCustomerId()).append("\n");
        prompt.append("- Name: ").append(request.getCustomerName()).append("\n");
        prompt.append("- Email: ").append(request.getCustomerEmail()).append("\n");
        prompt.append("- Account No: ").append(request.getCustomerAccountNo()).append("\n");
        prompt.append("- Account Balance: $").append(String.format("%.2f", request.getCustomerBalance())).append("\n");
        
        prompt.append("\nPREVIOUS TRANSACTION ANALYSIS:\n");
        prompt.append("- Average Transaction Amount: $").append(String.format("%.2f", averageAmount)).append("\n");
        prompt.append("- Most Common Location (State): ").append(previousLocation).append("\n");
        prompt.append("- Number of Previous Transactions: ").append(request.getPreviousTransactions().size()).append("\n");
        
        prompt.append("\nVALIDATION CHECKS APPLIED:\n");
        double threshold = averageAmount + (averageAmount * 0.05);
        prompt.append("- Check 1: Current amount ($").append(String.format("%.2f", request.getAmount()))
               .append(") vs Average + 5% ($").append(String.format("%.2f", threshold))
               .append("): ");
        if (request.getAmount() <= threshold) {
            prompt.append("FLAGGED - Risk Score increased by 10\n");
        } else {
            prompt.append("PASSED\n");
        }
        
        prompt.append("- Check 2: Location Match - Current (").append(extractStateName(request.getLocation()))
               .append(") vs Previous (").append(previousLocation).append("): ");
        if (extractStateName(request.getLocation()).equalsIgnoreCase(previousLocation)) {
            prompt.append("FLAGGED - Risk Score increased by 20\n");
        } else {
            prompt.append("PASSED\n");
        }
        
        prompt.append("\nCURRENT RISK SCORE: ").append(String.format("%.2f", updatedRiskScore)).append("\n");
        
        prompt.append("\nBased on the above analysis, provide your fraud assessment make sure the risk score must be incremented only if it not matches the given two validations.\n\n");
        
        prompt.append("IMPORTANT: You MUST respond ONLY with a valid JSON object (no markdown, no code blocks, no additional text).\n");
        prompt.append("JSON Format (MANDATORY):\n");
        prompt.append("{\n");
        prompt.append("  \"riskScore\": <number>,\n");
        prompt.append("  \"decision\": \"<Genuine|flagged|terminated>\",\n");
        prompt.append("  \"reason\": \"<clear explanation of why this decision was made based on the analysis and maximum length would be 250 characters only>\"\n");
        prompt.append("}\n\n");
        
        prompt.append("Decision Guidelines:\n");
        prompt.append("- Genuine: Low risk, normal transaction pattern\n");
        prompt.append("- Flagged: Medium risk, warrants manual review\n");
        prompt.append("- Terminated: High risk, transaction should be blocked\n");
        
        return prompt.toString();
    }

    /**
     * Call Gemini API with the prompt
     */
    public String askGemini(String prompt) throws HttpException, IOException {
        GenerateContentResponse response = client.models.generateContent(
                "gemini-3-flash-preview",
                prompt,
                  null);
        
        return response.text();
    }

    /**
     * Parse Gemini response and convert to GeminiDecisionResponse
     */
    private GeminiDecisionResponse parseGeminiResponse(String geminiResponse) {
        GeminiDecisionResponse response = new GeminiDecisionResponse();
        
        try {
            // Try to extract JSON from the response
            String jsonString = extractJsonFromResponse(geminiResponse);
            
            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            
            response.setRiskScore(jsonNode.get("riskScore").asDouble());
            response.setDecision(jsonNode.get("decision").asText());
            response.setReason(jsonNode.get("reason").asText());
            
        } catch (Exception e) {
            // Fallback parsing if JSON extraction/parsing fails
            response.setRiskScore(0.0);
            response.setDecision("flagged");
            response.setReason("Unable to parse Gemini response properly. Raw response: " + geminiResponse);
        }
        
        return response;
    }

    /**
     * Extract JSON object from response text
     */
    private String extractJsonFromResponse(String response) {
        // Try to find JSON object in the response
        Pattern pattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}");
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            return matcher.group();
        }

        // If no JSON found, wrap the response as JSON
        throw new RuntimeException("No JSON object found in response: " + response);
    }
}
