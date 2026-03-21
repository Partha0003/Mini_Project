package com.bank.frauddetection.dto;

import java.time.LocalDateTime;

public class FraudAlert {

    private String accountNumber;
    private Double amount;
    private String reason;
    private LocalDateTime timestamp;

    public FraudAlert() {
    }

    public FraudAlert(String accountNumber, Double amount, String reason, LocalDateTime timestamp) {
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
