package com.bank.frauddetection.repository;

import com.bank.frauddetection.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByAccountNumberAndTransactionTimeAfter(
            String accountNumber, LocalDateTime time);

    List<Transaction> findByAccountNumber(String accountNumber);

    List<Transaction> findByStatus(String status);

    Page<Transaction> findByStatus(String status, Pageable pageable);

    Page<Transaction> findByTransactionTimeBetween(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Transaction> findByAccountNumberContainingIgnoreCase(
            String accountNumber, Pageable pageable);
}
