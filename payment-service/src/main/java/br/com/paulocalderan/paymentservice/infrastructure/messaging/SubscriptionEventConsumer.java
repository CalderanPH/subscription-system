package br.com.paulocalderan.paymentservice.infrastructure.messaging;

import br.com.paulocalderan.paymentservice.application.service.PaymentService;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedFailedEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedSuccessEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.SubscriptionCreatedEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.SubscriptionRenewalRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    @KafkaListener(topics = "subscription-events", groupId = "payment-service-group")
    public void consumeSubscriptionEvent(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        try {
            Object eventValue = record.value();
            
            log.debug("Received subscription event: key={}, eventType={}", 
                    record.key(), 
                    eventValue != null ? eventValue.getClass().getSimpleName() : "null");

            if (eventValue instanceof java.util.Map<?, ?> eventMap) {
                String eventType = (String) eventMap.get("eventType");
                log.debug("Received event as Map with eventType: {}", eventType);
                
                if ("SUBSCRIPTION_CREATED".equals(eventType)) {
                    UUID subscriptionId = UUID.fromString(eventMap.get("subscriptionId").toString());
                    UUID userId = UUID.fromString(eventMap.get("userId").toString());
                    String plan = eventMap.get("plan").toString();
                    BigDecimal value = new BigDecimal(eventMap.get("value").toString());
                    
                    log.info("Processing payment for new subscription: {} (eventType: {})", subscriptionId, eventType);
                    processPayment(subscriptionId, userId, plan, value);
                } else if ("SUBSCRIPTION_RENEWAL_REQUESTED".equals(eventType)) {
                    UUID subscriptionId = UUID.fromString(eventMap.get("subscriptionId").toString());
                    UUID userId = UUID.fromString(eventMap.get("userId").toString());
                    String plan = eventMap.get("plan").toString();
                    BigDecimal value = new BigDecimal(eventMap.get("value").toString());
                    
                    log.info("Processing payment for subscription renewal: {} (eventType: {})", subscriptionId, eventType);
                    processPayment(subscriptionId, userId, plan, value);
                } else {
                    log.debug("Ignoring notification event that doesn't require payment processing: {}", eventType);
                    acknowledgment.acknowledge();
                    return;
                }
            } else {
                switch (eventValue) {
                    case SubscriptionCreatedEvent event -> {
                        log.info("Processing payment for new subscription: {}", event.subscriptionId());
                        processPayment(event.subscriptionId(), event.userId(), event.plan(), event.value());
                    }
                    case SubscriptionRenewalRequestedEvent event -> {
                        log.info("Processing payment for subscription renewal: {}", event.subscriptionId());
                        processPayment(event.subscriptionId(), event.userId(), event.plan(), event.value());
                    }
                    default -> {
                        log.debug("Ignoring event type that doesn't require payment processing: {}", 
                                eventValue != null ? eventValue.getClass().getSimpleName() : "null");
                        acknowledgment.acknowledge();
                        return;
                    }
                }
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing subscription event with key: {}", record.key(), e);
            throw e;
        }
    }

    private void processPayment(UUID subscriptionId, UUID userId, String plan, BigDecimal value) {
        log.info("Processing payment for subscription: {}", subscriptionId);

        boolean paymentSuccess = paymentService.processPayment(subscriptionId, userId, plan, value);

        if (paymentSuccess) {
            PaymentProcessedSuccessEvent successEvent = new PaymentProcessedSuccessEvent(subscriptionId, userId);
            paymentEventProducer.publishPaymentSuccess(successEvent);
            log.info("Payment success event published for subscription: {}", subscriptionId);
        } else {
            PaymentProcessedFailedEvent failedEvent = new PaymentProcessedFailedEvent(
                    subscriptionId, userId, "Payment processing failed: Insufficient funds or gateway error");
            paymentEventProducer.publishPaymentFailed(failedEvent);
            log.info("Payment failed event published for subscription: {}", subscriptionId);
            throw new RuntimeException("Payment failed for subscription: " + subscriptionId);
        }
    }
}

