package com.example.offlinePayment.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service using Token Bucket algorithm
 * SDE2 Skill: Security, Rate limiting, Algorithm implementation
 */
@Service
public class RateLimitingService {

    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    // 10 requests per minute per user
    private final Bandwidth userLimit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
    
    // 100 requests per minute per IP
    private final Bandwidth ipLimit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));

    public boolean isAllowedForUser(String userId) {
        Bucket bucket = userBuckets.computeIfAbsent(userId, k -> Bucket4j.builder()
                .addLimit(userLimit)
                .build());
        
        return bucket.tryConsume(1);
    }

    public boolean isAllowedForIP(String ipAddress) {
        Bucket bucket = ipBuckets.computeIfAbsent(ipAddress, k -> Bucket4j.builder()
                .addLimit(ipLimit)
                .build());
        
        return bucket.tryConsume(1);
    }

    public long getAvailableTokensForUser(String userId) {
        Bucket bucket = userBuckets.get(userId);
        return bucket != null ? bucket.getAvailableTokens() : userLimit.getCapacity();
    }
}