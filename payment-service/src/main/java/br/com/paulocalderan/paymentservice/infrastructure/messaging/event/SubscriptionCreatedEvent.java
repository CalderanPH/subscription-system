package br.com.paulocalderan.paymentservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeName("SUBSCRIPTION_CREATED")
public record SubscriptionCreatedEvent(
    UUID subscriptionId,
    UUID userId,
    String plan,
    BigDecimal value,
    String eventType,
    LocalDateTime timestamp
) implements SubscriptionEvent {
    public SubscriptionCreatedEvent(UUID subscriptionId, UUID userId, String plan, BigDecimal value) {
        this(subscriptionId, userId, plan, value, "SUBSCRIPTION_CREATED", LocalDateTime.now());
    }
}

