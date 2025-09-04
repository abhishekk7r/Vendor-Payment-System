package com.example.offlinePayment.service;

import com.example.offlinePayment.model.Transaction;
import com.example.offlinePayment.model.TransactionStatus;
import com.example.offlinePayment.model.PaymentMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling offline-ready transactions with eventual consistency
 * Demonstrates: Async processing, Redis caching, Scheduled tasks, Error handling
 */
@Service
@Slf4j
public class OfflineTransactionService {

    private static final String PENDING_TRANSACTIONS_KEY = "pending_transactions";
    private static final String OFFLINE_CODES_KEY = "offline_codes:";
    private static final String USER_BALANCE_KEY = "user_balance:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Store transaction for offline processing
     * SDE2 Skill: Async processing with CompletableFuture
     */
    @Async
    public CompletableFuture<String> storeOfflineTransaction(Transaction transaction) {
        try {
            // Store in Redis for offline processing
            String transactionKey = "offline_tx:" + transaction.getTransactionId();
            redisTemplate.opsForValue().set(transactionKey, transaction, 24, TimeUnit.HOURS);
            
            // Add to pending queue
            redisTemplate.opsForSet().add(PENDING_TRANSACTIONS_KEY, transactionKey);
            
            log.info("Stored offline transaction: {}", transaction.getTransactionId());
            return CompletableFuture.completedFuture("Transaction stored successfully");
            
        } catch (Exception e) {
            log.error("Failed to store offline transaction", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Batch process pending transactions
     * SDE2 Skill: Scheduled tasks, Batch processing, Error resilience
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void processPendingTransactions() {
        try {
            Set<Object> pendingKeys = redisTemplate.opsForSet().members(PENDING_TRANSACTIONS_KEY);
            
            if (pendingKeys == null || pendingKeys.isEmpty()) {
                return;
            }

            log.info("Processing {} pending transactions", pendingKeys.size());
            
            for (Object key : pendingKeys) {
                processTransaction((String) key);
            }
            
        } catch (Exception e) {
            log.error("Error processing pending transactions", e);
        }
    }

    private void processTransaction(String transactionKey) {
        try {
            Transaction transaction = (Transaction) redisTemplate.opsForValue().get(transactionKey);
            
            if (transaction == null) {
                redisTemplate.opsForSet().remove(PENDING_TRANSACTIONS_KEY, transactionKey);
                return;
            }

            // Simulate processing logic
            if (isTransactionValid(transaction)) {
                transaction.setStatus(TransactionStatus.SUCCESSFUL);
                log.info("Transaction {} processed successfully", transaction.getTransactionId());
            } else {
                transaction.setStatus(TransactionStatus.FAILED);
                log.warn("Transaction {} failed validation", transaction.getTransactionId());
            }

            // Remove from pending queue
            redisTemplate.opsForSet().remove(PENDING_TRANSACTIONS_KEY, transactionKey);
            redisTemplate.delete(transactionKey);
            
        } catch (Exception e) {
            log.error("Error processing transaction: {}", transactionKey, e);
        }
    }

    private boolean isTransactionValid(Transaction transaction) {
        // Add complex validation logic here
        return transaction.getAmount() > 0 && transaction.getUserId() != null;
    }

    /**
     * Cache offline codes for quick access
     * SDE2 Skill: Caching strategies, Performance optimization
     */
    public void cacheOfflineCodes(String userId, Set<String> codes) {
        String key = OFFLINE_CODES_KEY + userId;
        redisTemplate.opsForSet().add(key, codes.toArray());
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    public boolean validateOfflineCode(String userId, String code) {
        String key = OFFLINE_CODES_KEY + userId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, code));
    }
}