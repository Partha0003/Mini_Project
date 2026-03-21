package com.bank.frauddetection.dto;

import java.util.ArrayList;
import java.util.List;

public class FraudCheckResult {

    private int totalRisk;
    private List<FraudRuleResult> ruleResults = new ArrayList<>();

    public FraudCheckResult() {
    }

    public FraudCheckResult(int totalRisk, List<FraudRuleResult> ruleResults) {
        this.totalRisk = totalRisk;
        this.ruleResults = ruleResults;
    }

    public int getTotalRisk() {
        return totalRisk;
    }

    public void setTotalRisk(int totalRisk) {
        this.totalRisk = totalRisk;
    }

    public List<FraudRuleResult> getRuleResults() {
        return ruleResults;
    }

    public void setRuleResults(List<FraudRuleResult> ruleResults) {
        this.ruleResults = ruleResults;
    }
}
