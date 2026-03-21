package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.FraudCheckResult;
import com.bank.frauddetection.dto.FraudRuleResult;
import com.bank.frauddetection.ml.FraudMLPlugin;
import com.bank.frauddetection.model.FraudStatus;
import com.bank.frauddetection.model.Transaction;
import com.bank.frauddetection.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FraudDetectionService {

    @Autowired
    private TransactionRepository transactionRepository;

    private static final double HIGH_AMOUNT = 50000;
    private static final int RAPID_TX_LIMIT = 3;
    private static final int TIME_WINDOW = 2;
    private static final String RULE_HIGH_AMOUNT = "HIGH_AMOUNT";
    private static final String RULE_RAPID_TRANSACTION = "RAPID_TRANSACTION";
    private static final String RULE_LOCATION_MISMATCH = "LOCATION_MISMATCH";
    private static final String RULE_ML = "ML_RULE";
    private static final Map<String, String> RULE_REASON_MESSAGES = createRuleReasonMessages();

    @Value("${fraud.ml.enabled:false}")
    private boolean mlEnabled;

    @Autowired(required = false)
    private FraudMLPlugin mlPlugin;

    public int calculateRisk(Transaction tx) {
        return analyzeTransaction(tx).getTotalRisk();
    }

    public FraudCheckResult analyzeTransaction(Transaction tx) {
        List<FraudRuleResult> ruleResults = new ArrayList<>();

        if (tx.getAmount() > HIGH_AMOUNT) {
            ruleResults.add(new FraudRuleResult(RULE_HIGH_AMOUNT, 50));
        }

        List<Transaction> recentTransactions =
                transactionRepository.findByAccountNumberAndTransactionTimeAfter(
                        tx.getAccountNumber(),
                        LocalDateTime.now().minusMinutes(TIME_WINDOW)
                );

        if (recentTransactions.size() >= RAPID_TX_LIMIT) {
            ruleResults.add(new FraudRuleResult(RULE_RAPID_TRANSACTION, 30));
        }

        List<Transaction> pastTransactions =
                transactionRepository.findByAccountNumber(tx.getAccountNumber());

        if (!pastTransactions.isEmpty()) {
            String lastLocation =
                    pastTransactions.get(pastTransactions.size() - 1).getLocation();

            if (!lastLocation.equalsIgnoreCase(tx.getLocation())) {
                ruleResults.add(new FraudRuleResult(RULE_LOCATION_MISMATCH, 20));
            }
        }

        if (mlEnabled && mlPlugin != null) {
            int mlRisk = mlPlugin.predictRisk(tx);
            if (mlRisk > 0) {
                ruleResults.add(new FraudRuleResult(RULE_ML, mlRisk));
            }
        }

        int totalRisk = Math.min(
                ruleResults.stream().mapToInt(FraudRuleResult::getRiskContribution).sum(),
                100
        );

        return new FraudCheckResult(totalRisk, ruleResults);
    }

    public FraudStatus detectStatus(int riskScore) {

        if (riskScore >= 70) {
            return FraudStatus.FRAUD;
        } else if (riskScore >= 30) {
            return FraudStatus.SUSPICIOUS;
        } else {
            return FraudStatus.NORMAL;
        }
    }

    public String generateReason(Transaction tx, int riskScore) {
        FraudCheckResult fallback = new FraudCheckResult();
        fallback.setTotalRisk(riskScore);
        fallback.setRuleResults(new ArrayList<>());
        return generateReason(fallback);
    }

    public String generateReason(List<FraudRuleResult> rules, int riskScore) {
        FraudCheckResult fallback = new FraudCheckResult();
        fallback.setTotalRisk(riskScore);
        fallback.setRuleResults(rules != null ? rules : new ArrayList<>());
        return generateReason(fallback);
    }

    public String generateReason(FraudCheckResult result) {
        if (result == null || result.getRuleResults() == null || result.getRuleResults().isEmpty()) {
            return "Normal transaction within safe limits";
        }

        FraudStatus severity = detectStatus(result.getTotalRisk());
        String prefix = severityPrefix(severity);

        StringBuilder reason = new StringBuilder(prefix).append("\n");
        List<String> messages = new ArrayList<>();
        for (FraudRuleResult rule : result.getRuleResults()) {
            if (rule == null || rule.getRiskContribution() <= 0 || rule.getRuleName() == null) {
                continue;
            }

            String ruleName = rule.getRuleName().trim();
            String readable = RULE_REASON_MESSAGES.getOrDefault(
                    ruleName,
                    ruleName.replace("_", " ")
            );

            if (!messages.contains(readable)) {
                messages.add(readable);
            }
        }

        if (messages.isEmpty()) {
            return "Normal transaction within safe limits";
        }

        for (int i = 0; i < messages.size(); i++) {
            reason.append("- ").append(messages.get(i));
            if (i < messages.size() - 1) {
                reason.append("\n");
            }
        }

        return reason.toString();
    }

    private String severityPrefix(FraudStatus status) {
        String level = "Moderate";
        if (status == FraudStatus.FRAUD) {
            level = "High";
        }
        return level + " risk due to:";
    }

    private static Map<String, String> createRuleReasonMessages() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(RULE_HIGH_AMOUNT, "Transaction amount exceeds \u20B950,000");
        map.put(RULE_RAPID_TRANSACTION, "Multiple transactions within 2 minutes");
        map.put(RULE_LOCATION_MISMATCH, "Location differs from previous transaction");
        map.put(RULE_ML, "ML model flagged unusual behavior");
        return map;
    }
}