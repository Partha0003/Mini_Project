package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.FraudAlert;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
public class FraudAlertService {

    private static final int MAX_ALERTS = 10;
    private final Deque<FraudAlert> recentAlerts = new ArrayDeque<>();

    public synchronized void addAlert(String accountNumber, Double amount, String reason) {
        FraudAlert alert = new FraudAlert(accountNumber, amount, reason, LocalDateTime.now());
        recentAlerts.addFirst(alert);
        while (recentAlerts.size() > MAX_ALERTS) {
            recentAlerts.removeLast();
        }
    }

    public synchronized List<FraudAlert> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }
}
