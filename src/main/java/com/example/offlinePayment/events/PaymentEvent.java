package com.example.offlinePayment.events;

import com.example.offlinePayment.model.PaymentMode;
import com.example.offlinePayment.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payment event for event-driven architecture
 * SDE2 Skill: Event-driven design, Microservices communication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String eventId;
    private String transactionId;
    private String userId;
    private Integer vendorId;
    private Double amount;
    private PaymentMode paymentMode;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private String eventType; // PAYMENT_INITIATED, PAYMENT_COMPLETED, PAYMENT_FAILED, FRAUD_DETECTED
    private String metadata;
}