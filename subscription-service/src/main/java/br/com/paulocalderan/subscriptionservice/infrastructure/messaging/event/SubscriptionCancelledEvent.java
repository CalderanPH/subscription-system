package br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeName("SUBSCRIPTION_CANCELLED")
public record SubscriptionCancelledEvent(
    UUID subscriptionId,
    UUID userId,
    String eventType,
    LocalDateTime timestamp
) implements SubscriptionEvent {
    public SubscriptionCancelledEvent(UUID subscriptionId, UUID userId) {
        this(subscriptionId, userId, "SUBSCRIPTION_CANCELLED", LocalDateTime.now());
    }
}

