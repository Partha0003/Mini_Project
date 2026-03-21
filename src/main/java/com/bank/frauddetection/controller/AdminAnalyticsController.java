package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.FraudAlert;
import com.bank.frauddetection.dto.TransactionFilterDTO;
import com.bank.frauddetection.service.AdminAnalyticsService;
import com.bank.frauddetection.service.FraudAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
public class AdminAnalyticsController {

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @Autowired
    private FraudAlertService fraudAlertService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/analytics")
    @ResponseBody
    public Map<String, Object> getAdminAnalytics(
            @ModelAttribute TransactionFilterDTO filter,
            @RequestParam(defaultValue = "filtered") String mode,
            @RequestParam(defaultValue = "auto") String granularity,
            @RequestParam(defaultValue = "count") String metric) {

        return adminAnalyticsService.getAnalytics(filter, mode, granularity, metric);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/alerts")
    @ResponseBody
    public List<FraudAlert> getRecentAlerts() {
        return fraudAlertService.getRecentAlerts();
    }
}
