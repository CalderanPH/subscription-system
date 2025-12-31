package br.com.paulocalderan.paymentservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeName("PAYMENT_PROCESSED_SUCCESS")
public record PaymentProcessedSuccessEvent(
    UUID subscriptionId,
    UUID userId,
    String eventType,
    LocalDateTime timestamp
) {
    public PaymentProcessedSuccessEvent(UUID subscriptionId, UUID userId) {
        this(subscriptionId, userId, "PAYMENT_PROCESSED_SUCCESS", LocalDateTime.now());
    }
}

