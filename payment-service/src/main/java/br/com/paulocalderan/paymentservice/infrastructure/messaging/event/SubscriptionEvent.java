package br.com.paulocalderan.paymentservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SubscriptionCreatedEvent.class, name = "SUBSCRIPTION_CREATED"),
    @JsonSubTypes.Type(value = SubscriptionRenewalRequestedEvent.class, name = "SUBSCRIPTION_RENEWAL_REQUESTED")
})
public sealed interface SubscriptionEvent permits SubscriptionCreatedEvent, SubscriptionRenewalRequestedEvent {
    UUID subscriptionId();
    UUID userId();
    String eventType();
    LocalDateTime timestamp();
}

