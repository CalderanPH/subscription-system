package br.com.paulocalderan.paymentservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeName("PAYMENT_PROCESSED_FAILED")
public record PaymentProcessedFailedEvent(
    UUID subscriptionId,
    UUID userId,
    String reason,
    String eventType,
    LocalDateTime timestamp
) {
    public PaymentProcessedFailedEvent(UUID subscriptionId, UUID userId, String reason) {
        this(subscriptionId, userId, reason, "PAYMENT_PROCESSED_FAILED", LocalDateTime.now());
    }
}

