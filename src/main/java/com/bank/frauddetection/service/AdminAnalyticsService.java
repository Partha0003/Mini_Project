package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.TransactionFilterDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminAnalyticsService {

    @Autowired
    private AdminDashboardService adminDashboardService;

    public Map<String, Object> getAnalytics(
            TransactionFilterDTO filter,
            String mode,
            String granularity,
            String metric) {

        return new LinkedHashMap<>(
                adminDashboardService.buildAnalytics(filter, mode, granularity, metric)
        );
    }
}
