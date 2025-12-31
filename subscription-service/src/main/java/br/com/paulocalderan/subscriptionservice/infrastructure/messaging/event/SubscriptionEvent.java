package br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SubscriptionCreatedEvent.class, name = "SUBSCRIPTION_CREATED"),
    @JsonSubTypes.Type(value = SubscriptionRenewalRequestedEvent.class, name = "SUBSCRIPTION_RENEWAL_REQUESTED"),
    @JsonSubTypes.Type(value = SubscriptionRenewedEvent.class, name = "SUBSCRIPTION_RENEWED"),
    @JsonSubTypes.Type(value = SubscriptionCancelledEvent.class, name = "SUBSCRIPTION_CANCELLED"),
    @JsonSubTypes.Type(value = SubscriptionSuspendedEvent.class, name = "SUBSCRIPTION_SUSPENDED")
})
public sealed interface SubscriptionEvent permits SubscriptionCreatedEvent, SubscriptionRenewalRequestedEvent, 
        SubscriptionRenewedEvent, SubscriptionCancelledEvent, SubscriptionSuspendedEvent {
    UUID subscriptionId();
    UUID userId();
    String eventType();
    LocalDateTime timestamp();
}

