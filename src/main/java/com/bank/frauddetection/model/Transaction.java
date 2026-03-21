package com.bank.frauddetection.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String accountNumber;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double amount;

    @NotBlank
    private String location;

    private LocalDateTime transactionTime;

    private String status;

    private Integer riskScore;

    // ✅ NEW FIELD
    private String fraudReason;

    @PrePersist
    public void prePersist() {
        if (this.transactionTime == null) {
            this.transactionTime = LocalDateTime.now();
        }
    }

    // ===== GETTERS & SETTERS =====

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Double getAmount() { return amount; }

    public void setAmount(Double amount) { this.amount = amount; }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getTransactionTime() { return transactionTime; }

    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Integer getRiskScore() { return riskScore; }

    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getFraudReason() { return fraudReason; }

    public void setFraudReason(String fraudReason) { this.fraudReason = fraudReason; }
}