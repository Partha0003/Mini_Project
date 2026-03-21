package com.bank.frauddetection.repository;

import com.bank.frauddetection.model.FraudLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {
    List<FraudLog> findByTransactionId(Long transactionId);

    @Query("""
            SELECT f.transactionId as transactionId, COUNT(f) as ruleCount
            FROM FraudLog f
            WHERE f.transactionId IN :transactionIds
            GROUP BY f.transactionId
            """)
    List<RuleCountProjection> countRulesByTransactionIds(@Param("transactionIds") List<Long> transactionIds);

    interface RuleCountProjection {
        Long getTransactionId();
        Long getRuleCount();
    }
}
