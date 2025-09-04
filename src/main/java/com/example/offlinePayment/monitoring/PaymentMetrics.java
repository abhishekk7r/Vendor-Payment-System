package com.example.offlinePayment.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Custom metrics for payment system monitoring
 * SDE2 Skill: Observability, Metrics, Production monitoring
 */
@Component
public class PaymentMetrics {

    private final Counter onlinePaymentCounter;
    private final Counter offlinePaymentCounter;
    private final Counter failedPaymentCounter;
    private final Counter flaggedPaymentCounter;
    private final Timer paymentProcessingTimer;
    private final Counter fraudDetectionCounter;

    @Autowired
    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.onlinePaymentCounter = Counter.builder("payments.online.total")
                .description("Total online payments processed")
                .register(meterRegistry);

        this.offlinePaymentCounter = Counter.builder("payments.offline.total")
                .description("Total offline payments processed")
                .register(meterRegistry);

        this.failedPaymentCounter = Counter.builder("payments.failed.total")
                .description("Total failed payments")
                .register(meterRegistry);

        this.flaggedPaymentCounter = Counter.builder("payments.flagged.total")
                .description("Total flagged payments for review")
                .register(meterRegistry);

        this.paymentProcessingTimer = Timer.builder("payments.processing.duration")
                .description("Payment processing duration")
                .register(meterRegistry);

        this.fraudDetectionCounter = Counter.builder("fraud.detection.total")
                .description("Total fraud detection triggers")
                .register(meterRegistry);
    }

    public void incrementOnlinePayments() {
        onlinePaymentCounter.increment();
    }

    public void incrementOfflinePayments() {
        offlinePaymentCounter.increment();
    }

    public void incrementFailedPayments() {
        failedPaymentCounter.increment();
    }

    public void incrementFlaggedPayments() {
        flaggedPaymentCounter.increment();
    }

    public void incrementFraudDetection() {
        fraudDetectionCounter.increment();
    }

    public Timer.Sample startPaymentTimer() {
        return Timer.start();
    }

    public void recordPaymentTime(Timer.Sample sample) {
        sample.stop(paymentProcessingTimer);
    }
}