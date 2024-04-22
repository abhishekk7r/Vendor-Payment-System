package com.example.offlinePayment.repository;

import com.example.offlinePayment.model.Transaction;
import com.example.offlinePayment.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<List<Transaction>> findByStatus(TransactionStatus transactionStatus);
}
