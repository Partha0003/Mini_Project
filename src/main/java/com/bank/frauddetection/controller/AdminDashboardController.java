package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.TransactionFilterDTO;
import com.bank.frauddetection.model.Transaction;
import com.bank.frauddetection.service.AdminDashboardService;
import com.bank.frauddetection.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private TransactionService transactionService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            @ModelAttribute("filter") TransactionFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        applyDefaultDateRangeIfMissing(filter);

        Page<Transaction> transactionsPage = adminDashboardService
                .getPaginatedTransactions(filter, page, size);

        model.addAttribute("transactionsPage", transactionsPage);
        model.addAttribute("transactions", transactionsPage.getContent());
        model.addAttribute("currentPage", transactionsPage.getNumber());
        model.addAttribute("pageSize", transactionsPage.getSize());
        model.addAttribute("totalPages", transactionsPage.getTotalPages());
        model.addAttribute("totalElements", transactionsPage.getTotalElements());
        model.addAttribute("hasPrevious", transactionsPage.hasPrevious());
        model.addAttribute("hasNext", transactionsPage.hasNext());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("ruleCountMap", transactionService.getRuleCountMap(transactionsPage.getContent()));

        return "admin-dashboard";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/analytics")
    @ResponseBody
    public Map<String, Object> getAdminAnalytics(
            @ModelAttribute TransactionFilterDTO filter,
            @RequestParam(defaultValue = "filtered") String mode,
            @RequestParam(defaultValue = "auto") String granularity,
            @RequestParam(defaultValue = "count") String metric) {

        return new LinkedHashMap<>(
                adminDashboardService.buildAnalytics(filter, mode, granularity, metric)
        );
    }

    private void applyDefaultDateRangeIfMissing(TransactionFilterDTO filter) {
        if (filter.getFromDate() == null && filter.getToDate() == null) {
            filter.setToDate(LocalDate.now());
            filter.setFromDate(LocalDate.now().minusDays(30));
        }
    }
}
