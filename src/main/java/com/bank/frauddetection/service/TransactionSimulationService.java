package com.bank.frauddetection.service;

import com.bank.frauddetection.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class TransactionSimulationService {

    private static final String[] LOCATIONS = {
            "Delhi", "Mumbai", "London", "New York", "Dubai", "Singapore"
    };

    private static final String HEX = "0123456789ABCDEF";
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public List<Transaction> generateTransactions(int count) {
        Random random = new Random();
        List<Transaction> transactions = new ArrayList<>();

        // Requested demo behavior: for "simulate 10", force exact mix.
        if (count == 10) {
            for (int i = 0; i < 4; i++) {
                transactions.add(buildNormalTransaction(random));
            }
            for (int i = 0; i < 3; i++) {
                transactions.add(buildSuspiciousTransaction(random));
            }
            for (int i = 0; i < 3; i++) {
                transactions.add(buildFraudTransaction(random));
            }
            Collections.shuffle(transactions, random);
            return transactions;
        }

        for (int i = 0; i < count; i++) {
            if (random.nextBoolean()) {
                transactions.add(buildFraudTransaction(random));
            } else {
                transactions.add(buildNormalTransaction(random));
            }
        }

        return transactions;
    }

    private Transaction buildNormalTransaction(Random random) {
        Transaction tx = new Transaction();
        tx.setAccountNumber(generateValidAccount(random));
        tx.setLocation(LOCATIONS[random.nextInt(LOCATIONS.length)]);
        tx.setAmount(500.0 + random.nextInt(49000)); // below HIGH_AMOUNT
        return tx;
    }

    private Transaction buildSuspiciousTransaction(Random random) {
        Transaction tx = new Transaction();
        tx.setAccountNumber(generateValidAccount(random));
        tx.setLocation(LOCATIONS[random.nextInt(LOCATIONS.length)]);
        tx.setAmount(51000.0 + random.nextInt(9000)); // 51k-59,999 => +50
        return tx;
    }

    private Transaction buildFraudTransaction(Random random) {
        Transaction tx = new Transaction();
        tx.setAccountNumber(generateValidAccount(random));
        tx.setLocation(LOCATIONS[random.nextInt(LOCATIONS.length)]);
        tx.setAmount(61000.0 + random.nextInt(39000)); // >60k => HIGH_AMOUNT + ML risk
        return tx;
    }

    private String generateValidAccount(Random random) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }

        for (int i = 0; i < 4; i++) {
            sb.append(HEX.charAt(random.nextInt(HEX.length())));
        }

        return sb.toString();
    }
}