package br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PaymentProcessedSuccessEvent.class, name = "PAYMENT_PROCESSED_SUCCESS"),
    @JsonSubTypes.Type(value = PaymentProcessedFailedEvent.class, name = "PAYMENT_PROCESSED_FAILED")
})
public sealed interface PaymentEvent permits PaymentProcessedSuccessEvent, PaymentProcessedFailedEvent {
    UUID subscriptionId();
    UUID userId();
    String eventType();
    LocalDateTime timestamp();
}

