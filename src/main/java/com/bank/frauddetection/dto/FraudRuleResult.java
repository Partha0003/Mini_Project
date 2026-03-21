package com.bank.frauddetection.dto;

public class FraudRuleResult {

    private String ruleName;
    private int riskContribution;

    public FraudRuleResult() {
    }

    public FraudRuleResult(String ruleName, int riskContribution) {
        this.ruleName = ruleName;
        this.riskContribution = riskContribution;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public int getRiskContribution() {
        return riskContribution;
    }

    public void setRiskContribution(int riskContribution) {
        this.riskContribution = riskContribution;
    }
}
