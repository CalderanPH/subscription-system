package br.com.paulocalderan.paymentservice.infrastructure.messaging;

import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedFailedEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    public void publishPaymentSuccess(PaymentProcessedSuccessEvent event) {
        publishEvent(event.subscriptionId().toString(), event);
    }

    public void publishPaymentFailed(PaymentProcessedFailedEvent event) {
        publishEvent(event.subscriptionId().toString(), event);
    }

    private void publishEvent(String key, Object event) {
        try {
            kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, key, event).get();
            log.info("Event published successfully: {} to topic: {}", event, PAYMENT_EVENTS_TOPIC);
        } catch (Exception e) {
            log.error("Failed to publish event: {} to topic: {}", event, PAYMENT_EVENTS_TOPIC, e);
            throw new RuntimeException("Failed to publish event to Kafka: " + PAYMENT_EVENTS_TOPIC, e);
        }
    }
}

