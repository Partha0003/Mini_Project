package com.bank.frauddetection.service;

import com.bank.frauddetection.ml.FraudMLPlugin;
import com.bank.frauddetection.model.FraudLog;
import com.bank.frauddetection.model.FraudStatus;
import com.bank.frauddetection.model.Transaction;
import com.bank.frauddetection.repository.FraudLogRepository;
import com.bank.frauddetection.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudLogRepository fraudLogRepository;

    private static final double HIGH_AMOUNT = 50000;
    private static final int RAPID_TX_LIMIT = 3;
    private static final int TIME_WINDOW = 2;

    @Value("${fraud.ml.enabled:false}")
    private boolean mlEnabled;

    @Autowired(required = false)
    private FraudMLPlugin mlPlugin;

    public int calculateRisk(Transaction tx) {

        int ruleRisk = calculateRuleBasedRisk(tx);
        int mlRisk = 0;

        if (mlEnabled && mlPlugin != null) {
            mlRisk = mlPlugin.predictRisk(tx);
        }

        return Math.min(ruleRisk + mlRisk, 100);
    }

    private int calculateRuleBasedRisk(Transaction tx) {

        int riskScore = 0;

        if (tx.getAmount() > HIGH_AMOUNT) {
            riskScore += 50;
        }

        List<Transaction> recentTransactions =
                transactionRepository.findByAccountNumberAndTransactionTimeAfter(
                        tx.getAccountNumber(),
                        LocalDateTime.now().minusMinutes(TIME_WINDOW)
                );

        if (recentTransactions.size() >= RAPID_TX_LIMIT) {
            riskScore += 30;
        }

        List<Transaction> pastTransactions =
                transactionRepository.findByAccountNumber(tx.getAccountNumber());

        if (!pastTransactions.isEmpty()) {
            String lastLocation =
                    pastTransactions.get(pastTransactions.size() - 1).getLocation();

            if (!lastLocation.equalsIgnoreCase(tx.getLocation())) {
                riskScore += 20;
            }
        }

        return riskScore;
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

    // ✅ NEW METHOD
    public String generateReason(Transaction tx, int riskScore) {

        if (riskScore >= 70)
            return "High risk: Large amount or unusual behavior detected";

        else if (riskScore >= 30)
            return "Moderate risk: Suspicious transaction pattern";

        else
            return "Normal transaction within safe limits";
    }
}