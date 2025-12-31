package br.com.paulocalderan.paymentservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeName("SUBSCRIPTION_RENEWAL_REQUESTED")
public record SubscriptionRenewalRequestedEvent(
    UUID subscriptionId,
    UUID userId,
    String plan,
    BigDecimal value,
    String eventType,
    LocalDateTime timestamp
) implements SubscriptionEvent {
    public SubscriptionRenewalRequestedEvent(UUID subscriptionId, UUID userId, String plan, BigDecimal value) {
        this(subscriptionId, userId, plan, value, "SUBSCRIPTION_RENEWAL_REQUESTED", LocalDateTime.now());
    }
}

