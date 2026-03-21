package com.bank.frauddetection.controller;

import com.bank.frauddetection.model.FraudLog;
import com.bank.frauddetection.repository.FraudLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    @Autowired
    private FraudLogRepository fraudLogRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/logs")
    public List<FraudLog> getFraudLogs() {
        return fraudLogRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/logs/{transactionId}")
    public List<FraudLog> getFraudLogsByTransaction(@PathVariable Long transactionId) {
        return fraudLogRepository.findByTransactionId(transactionId);
    }
}
