package com.cts.gemini_test_try2.repository;

import com.cts.gemini_test_try2.entity.DecisionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DecisionRuleRepository extends JpaRepository<DecisionRule, Long> {
}
