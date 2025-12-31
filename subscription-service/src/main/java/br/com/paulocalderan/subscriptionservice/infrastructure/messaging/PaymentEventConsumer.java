package br.com.paulocalderan.subscriptionservice.infrastructure.messaging;

import br.com.paulocalderan.subscriptionservice.application.service.SubscriptionService;
import br.com.paulocalderan.subscriptionservice.domain.model.Status;
import br.com.paulocalderan.subscriptionservice.domain.model.Subscription;
import br.com.paulocalderan.subscriptionservice.domain.repository.SubscriptionRepository;
import br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event.PaymentProcessedFailedEvent;
import br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event.PaymentProcessedSuccessEvent;
import br.com.paulocalderan.subscriptionservice.infrastructure.metrics.SubscriptionMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventProducer subscriptionEventProducer;
    private final SubscriptionMetrics metrics;

    @KafkaListener(topics = "payment-events", groupId = "subscription-service-group")
    public void consumePaymentEvent(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        try {
            Object eventValue = record.value();
            log.info("Received payment event with key: {}, event type: {}, event: {}", 
                    record.key(), eventValue != null ? eventValue.getClass().getName() : "null", eventValue);

            if (eventValue instanceof PaymentProcessedSuccessEvent successEvent) {
                log.info("Detected PaymentProcessedSuccessEvent, processing...");
                handlePaymentSuccess(successEvent);
            } else if (eventValue instanceof PaymentProcessedFailedEvent failedEvent) {
                log.info("Detected PaymentProcessedFailedEvent, processing...");
                handlePaymentFailed(failedEvent);
            } else {
                log.warn("Unknown payment event type: {}, event: {}", 
                        eventValue != null ? eventValue.getClass().getName() : "null", eventValue);
                return;
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment event with key: {}", record.key(), e);
            throw e;
        }
    }

    private void handlePaymentSuccess(PaymentProcessedSuccessEvent event) {
        log.info("Processing payment success event for subscription: {}", event.subscriptionId());
        
        try {
            Subscription subscription = subscriptionRepository.findById(event.subscriptionId())
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + event.subscriptionId()));

            if (subscription.getStatus() == Status.PROCESSING) {
                subscription = subscriptionService.activateSubscription(event.subscriptionId());
                log.info("New subscription activated successfully: {}", event.subscriptionId());
            } else if (subscription.getStatus() == Status.ACTIVE) {
                subscription = subscriptionService.renewSubscription(event.subscriptionId());
                subscriptionEventProducer.publishSubscriptionRenewed(
                        new br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event.SubscriptionRenewedEvent(
                                subscription.getId(), subscription.getUserId()));
                log.info("Subscription renewed successfully: {}", event.subscriptionId());
            } else {
                log.warn("Subscription {} is not in PROCESSING or ACTIVE status, ignoring payment success", event.subscriptionId());
            }
        } catch (Exception e) {
            log.error("Error processing payment success for subscription: {}", event.subscriptionId(), e);
            throw e;
        }
    }

    private void handlePaymentFailed(PaymentProcessedFailedEvent event) {
        log.info("Processing payment failed event for subscription: {}, reason: {}", 
                event.subscriptionId(), event.reason());
        
        try {
            Subscription subscription = subscriptionRepository.findById(event.subscriptionId())
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + event.subscriptionId()));

            if (subscription.getStatus() == Status.PROCESSING) {
                subscription = subscriptionService.failSubscription(event.subscriptionId());
                log.info("New subscription payment failed, marked as FAILED: {}", event.subscriptionId());
            } else if (subscription.getStatus() == Status.ACTIVE) {
                subscription = subscriptionService.incrementFailedAttempt(event.subscriptionId());

                metrics.incrementRenewalError(subscription.getPlan().name());

                if (subscription.getStatus() == Status.SUSPENDED) {
                    subscriptionEventProducer.publishSubscriptionSuspended(
                            new br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event.SubscriptionSuspendedEvent(
                                    subscription.getId(), subscription.getUserId()));

                    log.info("Subscription suspended after 3 failed attempts: {}", event.subscriptionId());
                }
            } else {
                log.warn("Subscription {} is not in PROCESSING or ACTIVE status, ignoring payment failed", event.subscriptionId());
            }
        } catch (Exception e) {
            log.error("Error processing payment failed for subscription: {}", event.subscriptionId(), e);
            throw e;
        }
    }
}

