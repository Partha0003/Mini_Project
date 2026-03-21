package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.FraudCheckResult;
import com.bank.frauddetection.dto.FraudRuleResult;
import com.bank.frauddetection.dto.TransactionFilterDTO;
import com.bank.frauddetection.model.FraudLog;
import com.bank.frauddetection.model.FraudStatus;
import com.bank.frauddetection.model.Transaction;
import com.bank.frauddetection.repository.FraudLogRepository;
import com.bank.frauddetection.repository.TransactionRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Autowired
    private FraudLogRepository fraudLogRepository;

    @Autowired
    private FraudAlertService fraudAlertService;

    @EventListener(ApplicationReadyEvent.class)
    public void normalizeExistingAccountNumbersOnStartup() {
        transactionRepository.normalizeAllAccountNumbers();
    }

    public List<Transaction> getAll() {
        return transactionRepository.findAll();
    }

    public Page<Transaction> getAllTransactionsPage(int page, int size, String sortBy, String dir) {
        return transactionRepository.findAll(buildPageable(page, size, sortBy, dir));
    }

    public Page<Transaction> getTransactionsByStatusPage(String status, int page, int size, String sortBy, String dir) {
        return transactionRepository.findByStatus(status, buildPageable(page, size, sortBy, dir));
    }

    public Transaction createTransaction(Transaction transaction) {
        transaction.setAccountNumber(normalizeAccountNumber(transaction.getAccountNumber()));
        FraudCheckResult checkResult = fraudDetectionService.analyzeTransaction(transaction);
        int riskScore = checkResult.getTotalRisk();

        transaction.setRiskScore(riskScore);

        FraudStatus status = fraudDetectionService.detectStatus(riskScore);
        transaction.setStatus(status.name());

        String reason = fraudDetectionService.generateReason(checkResult);
        transaction.setFraudReason(reason);

        Transaction saved = transactionRepository.save(transaction);
        persistFraudRuleLogs(saved.getId(), checkResult.getRuleResults());
        if (FraudStatus.FRAUD.name().equals(saved.getStatus())) {
            fraudAlertService.addAlert(
                    saved.getAccountNumber(),
                    saved.getAmount(),
                    saved.getFraudReason()
            );
        }
        return saved;
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

    public Map<Long, Long> getRuleCountMap(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> txIds = transactions.stream()
                .map(Transaction::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (txIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>();
        List<FraudLogRepository.RuleCountProjection> counts =
                fraudLogRepository.countRulesByTransactionIds(txIds);
        for (FraudLogRepository.RuleCountProjection row : counts) {
            if (row.getTransactionId() != null && row.getRuleCount() != null) {
                result.put(row.getTransactionId(), row.getRuleCount());
            }
        }
        return result;
    }

    public Page<Transaction> getFilteredTransactions(TransactionFilterDTO filter, Pageable pageable) {
        return transactionRepository.findAll(buildFilterSpecification(filter), pageable);
    }

    public List<Transaction> getFilteredTransactions(TransactionFilterDTO filter) {
        return transactionRepository.findAll(
                buildFilterSpecification(filter),
                Sort.by(Sort.Direction.DESC, "transactionTime")
        );
    }

    private Specification<Transaction> buildFilterSpecification(TransactionFilterDTO filter) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (hasText(filter.getStatus())) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (hasText(filter.getAccountNumber())) {
                    String likeValue = "%" + filter.getAccountNumber().trim().toUpperCase() + "%";
                    predicates.add(cb.like(cb.upper(root.get("accountNumber")), likeValue));
                }

                if (filter.getFromDate() != null) {
                    LocalDateTime from = filter.getFromDate().atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("transactionTime"), from));
                }

                if (filter.getToDate() != null) {
                    LocalDateTime to = filter.getToDate().atTime(23, 59, 59);
                    predicates.add(cb.lessThanOrEqualTo(root.get("transactionTime"), to));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Pageable buildPageable(int page, int size, String sortBy, String dir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        String safeSortBy = switch (sortBy) {
            case "amount", "riskScore", "transactionTime", "accountNumber", "status" -> sortBy;
            default -> "transactionTime";
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(dir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    private void persistFraudRuleLogs(Long transactionId, List<FraudRuleResult> rules) {
        if (transactionId == null || rules == null || rules.isEmpty()) {
            return;
        }

        List<FraudLog> logs = new ArrayList<>();
        Set<String> uniqueRuleNames = new HashSet<>();

        for (FraudRuleResult rule : rules) {
            if (rule == null || rule.getRiskContribution() <= 0 || !hasText(rule.getRuleName())) {
                continue;
            }

            String ruleName = rule.getRuleName().trim();
            if (!uniqueRuleNames.add(ruleName)) {
                continue;
            }

            logs.add(new FraudLog(transactionId, ruleName, rule.getRiskContribution()));
        }

        if (!logs.isEmpty()) {
            fraudLogRepository.saveAll(logs);
        }
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            return null;
        }

        String cleaned = accountNumber.trim().toUpperCase();
        if (cleaned.isEmpty()) {
            return cleaned;
        }

        if (cleaned.length() >= 8) {
            return cleaned.substring(0, 8);
        }

        return "0".repeat(8 - cleaned.length()) + cleaned;
    }
}