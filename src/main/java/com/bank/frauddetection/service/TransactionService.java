package com.bank.frauddetection.service;

import com.bank.frauddetection.model.FraudStatus;
import com.bank.frauddetection.model.Transaction;
import com.bank.frauddetection.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    public List<Transaction> getAll() {
        return transactionRepository.findAll();
    }

    public Transaction createTransaction(Transaction transaction) {

        // Calculate Risk in Backend
        int riskScore = fraudDetectionService.calculateRisk(transaction);
        transaction.setRiskScore(riskScore);

        FraudStatus status = fraudDetectionService.detectStatus(riskScore);
        transaction.setStatus(status.name());

        String reason = fraudDetectionService.generateReason(transaction, riskScore);
        transaction.setFraudReason(reason);

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }

    public List<Transaction> getFraudTransactions() {
        return transactionRepository.findByStatus("FRAUD");
    }

    public List<Transaction> getSuspiciousTransactions() {
        return transactionRepository.findByStatus("SUSPICIOUS");
    }

    public List<Transaction> getNormalTransactions() {
        return transactionRepository.findByStatus("NORMAL");
    }
}