package br.com.paulocalderan.paymentservice.infrastructure.messaging;

import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedFailedEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedSuccessEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentEventProducer producer;

    private UUID subscriptionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void shouldPublishPaymentSuccessEvent() throws Exception {
        // Given
        PaymentProcessedSuccessEvent event = new PaymentProcessedSuccessEvent(subscriptionId, userId);
        SendResult<String, Object> sendResult = createSendResult();
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("payment-events"), eq(subscriptionId.toString()), any(PaymentProcessedSuccessEvent.class)))
                .thenReturn(future);

        // When
        producer.publishPaymentSuccess(event);

        // Then
        verify(kafkaTemplate).send(eq("payment-events"), eq(subscriptionId.toString()), any(PaymentProcessedSuccessEvent.class));
    }

    @Test
    void shouldPublishPaymentFailedEvent() throws Exception {
        // Given
        PaymentProcessedFailedEvent event = new PaymentProcessedFailedEvent(
                subscriptionId, userId, "Payment failed"
        );
        SendResult<String, Object> sendResult = createSendResult();
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("payment-events"), eq(subscriptionId.toString()), any(PaymentProcessedFailedEvent.class)))
                .thenReturn(future);

        // When
        producer.publishPaymentFailed(event);

        // Then
        verify(kafkaTemplate).send(eq("payment-events"), eq(subscriptionId.toString()), any(PaymentProcessedFailedEvent.class));
    }

    @Test
    void shouldThrowExceptionWhenPublishFails() throws Exception {
        // Given
        PaymentProcessedSuccessEvent event = new PaymentProcessedSuccessEvent(subscriptionId, userId);
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new ExecutionException("Kafka error", new RuntimeException()));

        when(kafkaTemplate.send(eq("payment-events"), eq(subscriptionId.toString()), any(PaymentProcessedSuccessEvent.class)))
                .thenReturn(future);

        // When/Then
        assertThatThrownBy(() -> producer.publishPaymentSuccess(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event to Kafka");
    }

    private SendResult<String, Object> createSendResult() {
        org.apache.kafka.clients.producer.ProducerRecord<String, Object> producerRecord = 
                new org.apache.kafka.clients.producer.ProducerRecord<>("payment-events", "key", "value");
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("payment-events", 0),
                0L,
                0,
                0L,
                0,
                0
        );
        return new SendResult<>(producerRecord, metadata);
    }
}
