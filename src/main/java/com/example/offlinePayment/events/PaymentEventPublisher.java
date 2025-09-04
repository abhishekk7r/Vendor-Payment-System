package com.example.offlinePayment.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event publisher for payment events
 * SDE2 Skill: Event sourcing, Pub/Sub patterns, Async messaging
 */
@Service
@Slf4j
public class PaymentEventPublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PAYMENT_EVENTS_CHANNEL = "payment_events";

    public void publishPaymentEvent(String transactionId, String userId, Integer vendorId, 
                                  Double amount, String eventType) {
        try {
            PaymentEvent event = PaymentEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .transactionId(transactionId)
                    .userId(userId)
                    .vendorId(vendorId)
                    .amount(amount)
                    .eventType(eventType)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Publish to Spring Application Events
            applicationEventPublisher.publishEvent(event);

            // Publish to Redis for external consumers
            redisTemplate.convertAndSend(PAYMENT_EVENTS_CHANNEL, event);

            log.info("Published payment event: {} for transaction: {}", eventType, transactionId);

        } catch (Exception e) {
            log.error("Failed to publish payment event", e);
        }
    }

    public void publishFraudDetectionEvent(String transactionId, String reason) {
        publishPaymentEvent(transactionId, null, null, null, "FRAUD_DETECTED");
        log.warn("Fraud detected for transaction: {} - Reason: {}", transactionId, reason);
    }
}