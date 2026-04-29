package com.neo.springapp.repository;

import com.neo.springapp.model.AiSecurityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiSecurityRuleRepository extends JpaRepository<AiSecurityRule, Long> {

    List<AiSecurityRule> findByIsActiveTrueOrderByPriorityAsc();

    List<AiSecurityRule> findByRuleCategoryAndIsActiveTrue(String ruleCategory);

    List<AiSecurityRule> findByChannelInAndIsActiveTrueOrderByPriorityAsc(List<String> channels);

    Optional<AiSecurityRule> findByRuleName(String ruleName);
}
