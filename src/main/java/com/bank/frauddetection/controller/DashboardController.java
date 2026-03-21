package com.bank.frauddetection.controller;

import com.bank.frauddetection.model.Transaction;
import com.bank.frauddetection.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private TransactionService transactionService;

    // ================= DASHBOARD =================
    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionTime") String sortBy,
            @RequestParam(defaultValue = "desc") String dir,
            Model model) {

        Page<Transaction> transactionsPage = transactionService.getAllTransactionsPage(page, size, sortBy, dir);
        List<Transaction> transactions = transactionsPage.getContent();
        List<Transaction> fraudTransactions = transactionService.getFraudTransactions();

        int totalTransactions = (int) transactionsPage.getTotalElements();
        int fraudCount = fraudTransactions.size();
        int safeCount = totalTransactions - fraudCount;
        Map<Long, Long> ruleCountMap = transactionService.getRuleCountMap(transactions);

        model.addAttribute("transactions", transactions);
        model.addAttribute("transactionsPage", transactionsPage);
        model.addAttribute("currentPage", transactionsPage.getNumber());
        model.addAttribute("totalPages", transactionsPage.getTotalPages());
        model.addAttribute("hasPrevious", transactionsPage.hasPrevious());
        model.addAttribute("hasNext", transactionsPage.hasNext());
        model.addAttribute("pageSize", transactionsPage.getSize());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("dir", dir);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("fraudCount", fraudCount);
        model.addAttribute("safeCount", safeCount);
        model.addAttribute("ruleCountMap", ruleCountMap);

        model.addAttribute("transaction", new Transaction());

        return "dashboard";
    }

    // ================= CREATE TRANSACTION =================
    @PostMapping("/transaction/create")
    public String createTransaction(
            @Valid @ModelAttribute("transaction") Transaction transaction,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            return dashboard(0, 10, "transactionTime", "desc", model);
        }

        transactionService.createTransaction(transaction);

        return "redirect:/dashboard";
    }

    // ================= REPORTS =================

    @GetMapping("/fraud-report")
    public String fraudReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionTime") String sortBy,
            @RequestParam(defaultValue = "desc") String dir,
            Model model) {

        Page<Transaction> transactionsPage = transactionService
                .getTransactionsByStatusPage("FRAUD", page, size, sortBy, dir);
        List<Transaction> transactions = transactionsPage.getContent();
        model.addAttribute("transactions", transactions);
        model.addAttribute("ruleCountMap", transactionService.getRuleCountMap(transactions));
        applyPaginationMeta(model, transactionsPage, size, sortBy, dir);
        model.addAttribute("basePath", "/fraud-report");
        return "fraud-report";
    }

    @GetMapping("/suspicious-report")
    public String suspiciousReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionTime") String sortBy,
            @RequestParam(defaultValue = "desc") String dir,
            Model model) {

        Page<Transaction> transactionsPage = transactionService
                .getTransactionsByStatusPage("SUSPICIOUS", page, size, sortBy, dir);
        List<Transaction> transactions = transactionsPage.getContent();
        model.addAttribute("transactions", transactions);
        model.addAttribute("ruleCountMap", transactionService.getRuleCountMap(transactions));
        applyPaginationMeta(model, transactionsPage, size, sortBy, dir);
        model.addAttribute("basePath", "/suspicious-report");
        return "suspicious-report";
    }

    @GetMapping("/normal-report")
    public String normalReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionTime") String sortBy,
            @RequestParam(defaultValue = "desc") String dir,
            Model model) {

        Page<Transaction> transactionsPage = transactionService
                .getTransactionsByStatusPage("NORMAL", page, size, sortBy, dir);
        List<Transaction> transactions = transactionsPage.getContent();
        model.addAttribute("transactions", transactions);
        model.addAttribute("ruleCountMap", transactionService.getRuleCountMap(transactions));
        applyPaginationMeta(model, transactionsPage, size, sortBy, dir);
        model.addAttribute("basePath", "/normal-report");
        return "normal-report";
    }

    private void applyPaginationMeta(Model model, Page<Transaction> pageData, int size, String sortBy, String dir) {
        model.addAttribute("transactionsPage", pageData);
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("hasPrevious", pageData.hasPrevious());
        model.addAttribute("hasNext", pageData.hasNext());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("dir", dir);
    }

    // ================= DELETE =================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/transaction/delete/{id}")
    public String deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return "redirect:/dashboard";
    }
}