package com.example.offlinePayment.service;

import com.example.offlinePayment.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for payment system optimizations
 * SDE2 Skill: Performance optimization, Caching, Async processing, Thread pools
 */
@Service
@Slf4j
public class PaymentOptimizationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Batch process multiple transactions for better performance
     */
    public CompletableFuture<Void> batchProcessTransactions(List<Transaction> transactions) {
        return CompletableFuture.runAsync(() -> {
            log.info("Processing batch of {} transactions", transactions.size());
            
            transactions.parallelStream().forEach(transaction -> {
                try {
                    processTransactionAsync(transaction);
                } catch (Exception e) {
                    log.error("Error processing transaction: {}", transaction.getTransactionId(), e);
                }
            });
            
            log.info("Completed batch processing");
        }, executorService);
    }

    /**
     * Cache frequently accessed user data
     */
    @Cacheable(value = "userCache", key = "#userId")
    public String getUserCachedData(String userId) {
        // Simulate expensive database operation
        log.info("Fetching user data from database for: {}", userId);
        return "User data for: " + userId;
    }

    /**
     * Preload user codes into cache for faster offline validation
     */
    public void preloadUserCodes(String userId, List<String> codes) {
        String cacheKey = "user_codes:" + userId;
        redisTemplate.opsForList().rightPushAll(cacheKey, codes.toArray());
        redisTemplate.expire(cacheKey, java.time.Duration.ofHours(24));
        log.info("Preloaded {} codes for user: {}", codes.size(), userId);
    }

    private void processTransactionAsync(Transaction transaction) {
        // Simulate transaction processing
        try {
            Thread.sleep(100); // Simulate processing time
            log.debug("Processed transaction: {}", transaction.getTransactionId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Transaction processing interrupted", e);
        }
    }

    /**
     * Circuit breaker pattern for external service calls
     */
    public boolean isExternalServiceHealthy() {
        // Implement circuit breaker logic
        return true;
    }
}