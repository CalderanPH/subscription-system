package br.com.paulocalderan.paymentservice.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final Random random = new Random();
    private static final double SUCCESS_RATE = 0.80;

    public boolean processPayment(UUID subscriptionId, UUID userId, String plan, BigDecimal value) {
        log.info("Processing payment for subscription: {}, user: {}, plan: {}, value: {}", 
                subscriptionId, userId, plan, value);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted", e);
            return false;
        }

        boolean success = random.nextDouble() < SUCCESS_RATE;

        if (success) {
            log.info("Payment processed successfully for subscription: {}", subscriptionId);
        } else {
            log.warn("Payment processing failed for subscription: {} - Insufficient funds or gateway error", 
                    subscriptionId);
        }

        return success;
    }
}

