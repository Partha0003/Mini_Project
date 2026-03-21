package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.TransactionFilterDTO;
import com.bank.frauddetection.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    @Autowired
    private TransactionService transactionService;

    public Page<Transaction> getPaginatedTransactions(TransactionFilterDTO filter, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "transactionTime"));
        return transactionService.getFilteredTransactions(filter, pageable);
    }

    public List<Transaction> getFilteredTransactionsForExport(TransactionFilterDTO filter) {
        return transactionService.getFilteredTransactions(filter);
    }

    public List<Transaction> getAllTransactionsForAnalytics() {
        return transactionService.getAll();
    }

    public Map<String, Long> getStatusCounts(TransactionFilterDTO filter) {
        List<Transaction> transactions = transactionService.getFilteredTransactions(filter);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("FRAUD", transactions.stream().filter(t -> "FRAUD".equals(t.getStatus())).count());
        statusCounts.put("SUSPICIOUS", transactions.stream().filter(t -> "SUSPICIOUS".equals(t.getStatus())).count());
        statusCounts.put("NORMAL", transactions.stream().filter(t -> "NORMAL".equals(t.getStatus())).count());
        return statusCounts;
    }

    public Map<String, Object> buildAnalytics(
            TransactionFilterDTO filter,
            String mode,
            String granularity,
            String metric) {

        List<Transaction> source = isFilteredMode(mode)
                ? transactionService.getFilteredTransactions(filter)
                : transactionService.getAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", isFilteredMode(mode) ? "filtered" : "overall");
        response.put("metric", normalizeMetric(metric));

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("FRAUD", source.stream().filter(t -> "FRAUD".equals(t.getStatus())).count());
        statusCounts.put("SUSPICIOUS", source.stream().filter(t -> "SUSPICIOUS".equals(t.getStatus())).count());
        statusCounts.put("NORMAL", source.stream().filter(t -> "NORMAL".equals(t.getStatus())).count());
        response.put("statusCounts", statusCounts);

        List<Transaction> dated = source.stream()
                .filter(t -> t.getTransactionTime() != null)
                .sorted(Comparator.comparing(Transaction::getTransactionTime))
                .collect(Collectors.toList());

        String resolvedGranularity = resolveGranularity(granularity, dated);
        response.put("granularity", resolvedGranularity);
        response.put("transactionsByDate", groupForTrend(dated, resolvedGranularity, normalizeMetric(metric)));

        long total = source.size();
        long fraud = statusCounts.get("FRAUD");
        double avgRisk = source.stream()
                .filter(t -> t.getRiskScore() != null)
                .mapToInt(Transaction::getRiskScore)
                .average()
                .orElse(0.0);
        long highRisk = source.stream()
                .filter(t -> t.getRiskScore() != null && t.getRiskScore() >= 70)
                .count();
        double fraudRate = total == 0 ? 0.0 : (fraud * 100.0) / total;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTransactions", total);
        summary.put("fraudTransactions", fraud);
        summary.put("highRiskTransactions", highRisk);
        summary.put("averageRiskScore", round2(avgRisk));
        summary.put("fraudRatePercent", round2(fraudRate));
        response.put("summary", summary);

        response.put("insights", buildInsights(source, fraudRate));
        response.put("topRiskyAccounts", buildTopRiskyAccounts(source, 5));
        response.put("hasEnoughTrendData", dated.size() >= 4 && uniqueDateBuckets(dated, resolvedGranularity) >= 2);

        return response;
    }

    private Map<String, Object> buildInsights(List<Transaction> source, double currentFraudRate) {
        Map<String, Object> insights = new LinkedHashMap<>();

        Transaction highestRisk = source.stream()
                .filter(t -> t.getRiskScore() != null)
                .max(Comparator.comparing(Transaction::getRiskScore))
                .orElse(null);

        Transaction latestFraud = source.stream()
                .filter(t -> "FRAUD".equals(t.getStatus()) && t.getTransactionTime() != null)
                .max(Comparator.comparing(Transaction::getTransactionTime))
                .orElse(null);

        List<Transaction> sortedByTime = source.stream()
                .filter(t -> t.getTransactionTime() != null)
                .sorted(Comparator.comparing(Transaction::getTransactionTime))
                .collect(Collectors.toList());
        int split = sortedByTime.size() / 2;
        List<Transaction> firstHalf = split > 0 ? sortedByTime.subList(0, split) : List.of();
        double previousFraudRate = calculateFraudRate(firstHalf);

        insights.put("highestRiskTransaction", toInsightTransaction(highestRisk));
        insights.put("latestFraudTransaction", toInsightTransaction(latestFraud));
        insights.put("fraudRateDelta", round2(currentFraudRate - previousFraudRate));
        insights.put("previousFraudRatePercent", round2(previousFraudRate));
        return insights;
    }

    private List<Map<String, Object>> buildTopRiskyAccounts(List<Transaction> source, int limit) {
        Map<String, List<Transaction>> byAccount = source.stream()
                .filter(t -> t.getAccountNumber() != null && !t.getAccountNumber().isBlank())
                .collect(Collectors.groupingBy(Transaction::getAccountNumber));

        return byAccount.entrySet().stream()
                .map(entry -> {
                    String account = entry.getKey();
                    List<Transaction> txns = entry.getValue();
                    double avgRisk = txns.stream()
                            .filter(t -> t.getRiskScore() != null)
                            .mapToInt(Transaction::getRiskScore)
                            .average()
                            .orElse(0.0);
                    long fraudCount = txns.stream().filter(t -> "FRAUD".equals(t.getStatus())).count();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("accountNumber", account);
                    row.put("avgRiskScore", round2(avgRisk));
                    row.put("fraudTransactions", fraudCount);
                    row.put("totalTransactions", txns.size());
                    return row;
                })
                .sorted((a, b) -> {
                    Double avgB = ((Number) b.get("avgRiskScore")).doubleValue();
                    Double avgA = ((Number) a.get("avgRiskScore")).doubleValue();
                    int byRisk = avgB.compareTo(avgA);
                    if (byRisk != 0) {
                        return byRisk;
                    }
                    Long fraudB = ((Number) b.get("fraudTransactions")).longValue();
                    Long fraudA = ((Number) a.get("fraudTransactions")).longValue();
                    return fraudB.compareTo(fraudA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    private double calculateFraudRate(List<Transaction> txns) {
        if (txns == null || txns.isEmpty()) {
            return 0.0;
        }
        long fraudCount = txns.stream().filter(t -> "FRAUD".equals(t.getStatus())).count();
        return (fraudCount * 100.0) / txns.size();
    }

    private Map<String, Object> toInsightTransaction(Transaction tx) {
        if (tx == null) {
            return null;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", tx.getId());
        value.put("accountNumber", tx.getAccountNumber());
        value.put("amount", tx.getAmount());
        value.put("riskScore", tx.getRiskScore());
        value.put("status", tx.getStatus());
        value.put("transactionTime", tx.getTransactionTime());
        return value;
    }

    private Map<String, Number> groupForTrend(
            List<Transaction> transactions,
            String granularity,
            String metric) {

        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            String key = bucketKey(tx.getTransactionTime(), granularity);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
        }

        Map<String, Number> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            List<Transaction> bucketTx = entry.getValue();
            result.put(entry.getKey(), metricValue(metric, bucketTx));
        }
        return result;
    }

    private Number metricValue(String metric, List<Transaction> bucketTx) {
        long total = bucketTx.size();
        long fraud = bucketTx.stream().filter(t -> "FRAUD".equals(t.getStatus())).count();

        switch (metric) {
            case "fraudCount":
                return fraud;
            case "avgRisk":
                return round2(bucketTx.stream()
                        .filter(t -> t.getRiskScore() != null)
                        .mapToInt(Transaction::getRiskScore)
                        .average()
                        .orElse(0.0));
            case "fraudRate":
                return total == 0 ? 0.0 : round2((fraud * 100.0) / total);
            case "count":
            default:
                return total;
        }
    }

    private String resolveGranularity(String requested, List<Transaction> dated) {
        String normalized = requested == null ? "auto" : requested.trim().toLowerCase(Locale.ROOT);
        if (!"auto".equals(normalized)) {
            return normalized;
        }
        if (dated.isEmpty()) {
            return "day";
        }

        LocalDateTime min = dated.get(0).getTransactionTime();
        LocalDateTime max = dated.get(dated.size() - 1).getTransactionTime();
        long days = ChronoUnit.DAYS.between(min.toLocalDate(), max.toLocalDate());

        if (days <= 1) {
            return "hour";
        }
        if (days <= 31) {
            return "day";
        }
        return "month";
    }

    private String bucketKey(LocalDateTime dateTime, String granularity) {
        switch (granularity) {
            case "hour":
                return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
            case "week":
                int week = dateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                return dateTime.getYear() + "-W" + String.format("%02d", week);
            case "month":
                return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "day":
            default:
                return dateTime.toLocalDate().toString();
        }
    }

    private boolean isFilteredMode(String mode) {
        return mode != null && mode.trim().equalsIgnoreCase("filtered");
    }

    private String normalizeMetric(String metric) {
        if (metric == null) {
            return "count";
        }
        String normalized = metric.trim();
        if (Objects.equals(normalized, "fraudCount")
                || Objects.equals(normalized, "avgRisk")
                || Objects.equals(normalized, "fraudRate")) {
            return normalized;
        }
        return "count";
    }

    private int uniqueDateBuckets(List<Transaction> dated, String granularity) {
        return (int) dated.stream()
                .map(t -> bucketKey(t.getTransactionTime(), granularity))
                .distinct()
                .count();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
