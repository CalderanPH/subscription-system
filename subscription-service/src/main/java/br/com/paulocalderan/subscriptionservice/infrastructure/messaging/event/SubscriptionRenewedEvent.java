package br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeName("SUBSCRIPTION_RENEWED")
public record SubscriptionRenewedEvent(
    UUID subscriptionId,
    UUID userId,
    String eventType,
    LocalDateTime timestamp
) implements SubscriptionEvent {
    public SubscriptionRenewedEvent(UUID subscriptionId, UUID userId) {
        this(subscriptionId, userId, "SUBSCRIPTION_RENEWED", LocalDateTime.now());
    }
}

