package com.cts.gemini_test_try2.Controller;

import com.cts.gemini_test_try2.entity.DecisionRule;
import com.cts.gemini_test_try2.repository.DecisionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD for the rules the Risk Manager maintains.
 *
 * Mounted under /api/gemini/rules so the existing /api/gemini/** gateway route
 * forwards here — no extra route config needed in the ConfigServer repo.
 */
@RestController
@RequestMapping("/api/gemini/rules")
@RequiredArgsConstructor
public class DecisionRuleController {

    private final DecisionRuleRepository ruleRepo;

    @GetMapping
    public List<DecisionRule> list() {
        return ruleRepo.findAll();
    }

    @PostMapping
    public DecisionRule add(@RequestBody DecisionRule body) {
        // The client only sends text + riskScore; id is generated.
        DecisionRule saved = ruleRepo.save(new DecisionRule(null, body.getText(), body.getRiskScore()));
        System.out.println("[DecisionRules] Added rule id=" + saved.getId()
                + " riskScore=" + saved.getRiskScore()
                + " text=\"" + saved.getText() + "\"");
        return saved;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!ruleRepo.existsById(id)) return ResponseEntity.notFound().build();
        ruleRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<DecisionRule> update(@PathVariable Long id, @RequestBody DecisionRule body) {
        return ruleRepo.findById(id)
                .map(existing -> {
                    if (body.getText() != null)      existing.setText(body.getText());
                    if (body.getRiskScore() != null) existing.setRiskScore(body.getRiskScore());
                    DecisionRule saved = ruleRepo.save(existing);
                    System.out.println("[DecisionRules] Updated rule id=" + saved.getId()
                            + " riskScore=" + saved.getRiskScore()
                            + " text=\"" + saved.getText() + "\"");
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
