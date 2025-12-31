package br.com.paulocalderan.subscriptionservice.infrastructure.messaging;

import br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String SUBSCRIPTION_EVENTS_TOPIC = "subscription-events";

    public void publishSubscriptionCreated(SubscriptionCreatedEvent event) {
        publishEvent(event.subscriptionId().toString(), event);
    }

    public void publishRenewalRequested(SubscriptionRenewalRequestedEvent event) {
        publishEvent(event.subscriptionId().toString(), event);
    }

    public void publishSubscriptionRenewed(SubscriptionRenewedEvent event) {
        publishEvent(event.subscriptionId().toString(), event);
    }

    public void publishSubscriptionCancelled(SubscriptionCancelledEvent event) {
        publishEvent(event.subscriptionId().toString(), event);
    }

    public void publishSubscriptionSuspended(SubscriptionSuspendedEvent event) {
        publishEvent(event.subscriptionId().toString(), event);
    }

    private void publishEvent(String key, Object event) {
        try {
            SendResult<String, Object> result = kafkaTemplate.send(
                    SUBSCRIPTION_EVENTS_TOPIC, key, event).get();
            log.info("Event published successfully: {} to topic: {}", event, SUBSCRIPTION_EVENTS_TOPIC);
        } catch (Exception e) {
            log.error("Failed to publish event: {} to topic: {}", event, SUBSCRIPTION_EVENTS_TOPIC, e);
            throw new RuntimeException("Failed to publish event to Kafka: " + SUBSCRIPTION_EVENTS_TOPIC, e);
        }
    }
}

