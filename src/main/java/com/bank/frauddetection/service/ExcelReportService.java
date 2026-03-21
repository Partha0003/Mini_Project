package com.bank.frauddetection.service;

import com.bank.frauddetection.model.Transaction;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ExcelReportService {

    @Autowired
    private TransactionService transactionService;

    public void generateFraudReport(HttpServletResponse response) throws IOException {
        List<Transaction> fraudList = transactionService.getFraudTransactions();
        generateTransactionReport(response, fraudList, "Fraud Transactions");
    }

    public void generateSuspiciousReport(HttpServletResponse response) throws IOException {
        List<Transaction> suspiciousList = transactionService.getSuspiciousTransactions();
        generateTransactionReport(response, suspiciousList, "Suspicious Transactions");
    }

    public void generateNormalReport(HttpServletResponse response) throws IOException {
        List<Transaction> normalList = transactionService.getNormalTransactions();
        generateTransactionReport(response, normalList, "Normal Transactions");
    }

    public void generateTransactionReport(
            HttpServletResponse response,
            List<Transaction> transactions,
            String sheetName) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(sheetName);

        int rowNum = 0;

        // Header Row
        Row header = sheet.createRow(rowNum++);
        header.createCell(0).setCellValue("Account Number");
        header.createCell(1).setCellValue("Amount");
        header.createCell(2).setCellValue("Location");
        header.createCell(3).setCellValue("Risk Score");
        header.createCell(4).setCellValue("Status");

        // Data Rows
        for (Transaction tx : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tx.getAccountNumber());
            row.createCell(1).setCellValue(tx.getAmount());
            row.createCell(2).setCellValue(tx.getLocation());
            row.createCell(3).setCellValue(tx.getRiskScore());
            row.createCell(4).setCellValue(tx.getStatus());
        }

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }
}