package br.com.paulocalderan.paymentservice.infrastructure.messaging;

import br.com.paulocalderan.paymentservice.application.service.PaymentService;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedFailedEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedSuccessEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.SubscriptionCreatedEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.SubscriptionRenewalRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private SubscriptionEventConsumer consumer;

    private UUID subscriptionId;
    private UUID userId;
    private String plan;
    private BigDecimal value;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        plan = "BASIC";
        value = new BigDecimal("19.90");
    }

    @Test
    void shouldProcessSubscriptionCreatedEventFromMap() {
        // Given
        Map<String, Object> eventMap = createSubscriptionCreatedMap();
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("subscription-events", 0, 0, "key", eventMap);

        when(paymentService.processPayment(any(UUID.class), any(UUID.class), anyString(), any(BigDecimal.class)))
                .thenReturn(true);

        // When
        consumer.consumeSubscriptionEvent(record, acknowledgment);

        // Then
        verify(paymentService).processPayment(subscriptionId, userId, plan, value);
        verify(paymentEventProducer).publishPaymentSuccess(any(PaymentProcessedSuccessEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldProcessSubscriptionRenewalRequestedEventFromMap() {
        // Given
        Map<String, Object> eventMap = createSubscriptionRenewalRequestedMap();
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("subscription-events", 0, 0, "key", eventMap);

        when(paymentService.processPayment(any(UUID.class), any(UUID.class), anyString(), any(BigDecimal.class)))
                .thenReturn(true);

        // When
        consumer.consumeSubscriptionEvent(record, acknowledgment);

        // Then
        verify(paymentService).processPayment(subscriptionId, userId, plan, value);
        verify(paymentEventProducer).publishPaymentSuccess(any(PaymentProcessedSuccessEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldPublishFailedEventWhenPaymentFails() {
        // Given
        Map<String, Object> eventMap = createSubscriptionCreatedMap();
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("subscription-events", 0, 0, "key", eventMap);

        when(paymentService.processPayment(any(UUID.class), any(UUID.class), anyString(), any(BigDecimal.class)))
                .thenReturn(false);

        // When/Then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            consumer.consumeSubscriptionEvent(record, acknowledgment);
        });

        // Then
        verify(paymentService).processPayment(subscriptionId, userId, plan, value);
        verify(paymentEventProducer).publishPaymentFailed(any(PaymentProcessedFailedEvent.class));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void shouldIgnoreSubscriptionCancelledEvent() {
        // Given
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventType", "SUBSCRIPTION_CANCELLED");
        eventMap.put("subscriptionId", subscriptionId.toString());
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("subscription-events", 0, 0, "key", eventMap);

        // When
        consumer.consumeSubscriptionEvent(record, acknowledgment);

        // Then
        verify(paymentService, never()).processPayment(any(), any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldIgnoreSubscriptionRenewedEvent() {
        // Given
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventType", "SUBSCRIPTION_RENEWED");
        eventMap.put("subscriptionId", subscriptionId.toString());
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("subscription-events", 0, 0, "key", eventMap);

        // When
        consumer.consumeSubscriptionEvent(record, acknowledgment);

        // Then
        verify(paymentService, never()).processPayment(any(), any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldProcessTypedSubscriptionCreatedEvent() {
        // Given
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                subscriptionId, userId, plan, value, null, null
        );
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("subscription-events", 0, 0, "key", event);

        when(paymentService.processPayment(any(UUID.class), any(UUID.class), anyString(), any(BigDecimal.class)))
                .thenReturn(true);

        // When
        consumer.consumeSubscriptionEvent(record, acknowledgment);

        // Then
        verify(paymentService).processPayment(subscriptionId, userId, plan, value);
        verify(paymentEventProducer).publishPaymentSuccess(any(PaymentProcessedSuccessEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldProcessTypedSubscriptionRenewalRequestedEvent() {
        // Given
        SubscriptionRenewalRequestedEvent event = new SubscriptionRenewalRequestedEvent(
                subscriptionId, userId, plan, value, null, null
        );
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("subscription-events", 0, 0, "key", event);

        when(paymentService.processPayment(any(UUID.class), any(UUID.class), anyString(), any(BigDecimal.class)))
                .thenReturn(true);

        // When
        consumer.consumeSubscriptionEvent(record, acknowledgment);

        // Then
        verify(paymentService).processPayment(subscriptionId, userId, plan, value);
        verify(paymentEventProducer).publishPaymentSuccess(any(PaymentProcessedSuccessEvent.class));
        verify(acknowledgment).acknowledge();
    }

    private Map<String, Object> createSubscriptionCreatedMap() {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventType", "SUBSCRIPTION_CREATED");
        eventMap.put("subscriptionId", subscriptionId.toString());
        eventMap.put("userId", userId.toString());
        eventMap.put("plan", plan);
        eventMap.put("value", value.toString());
        return eventMap;
    }

    private Map<String, Object> createSubscriptionRenewalRequestedMap() {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventType", "SUBSCRIPTION_RENEWAL_REQUESTED");
        eventMap.put("subscriptionId", subscriptionId.toString());
        eventMap.put("userId", userId.toString());
        eventMap.put("plan", plan);
        eventMap.put("value", value.toString());
        return eventMap;
    }
}
