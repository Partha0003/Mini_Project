package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.TransactionFilterDTO;
import com.bank.frauddetection.service.AdminDashboardService;
import com.bank.frauddetection.service.ExcelReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class ReportController {

    @Autowired
    private ExcelReportService excelReportService;

    @Autowired
    private AdminDashboardService adminDashboardService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/download-fraud-report")
    public void downloadFraudReport(HttpServletResponse response) throws IOException {

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=fraud_report.xlsx");

        excelReportService.generateFraudReport(response);  // Make sure method name same
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/export/excel")
    public void downloadFilteredTransactions(
            @ModelAttribute TransactionFilterDTO filter,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=admin_filtered_transactions.xlsx");

        excelReportService.generateTransactionReport(
                response,
                adminDashboardService.getFilteredTransactionsForExport(filter),
                "Filtered Transactions"
        );
    }
}