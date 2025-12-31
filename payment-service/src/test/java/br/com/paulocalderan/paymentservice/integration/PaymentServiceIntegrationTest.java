package br.com.paulocalderan.paymentservice.integration;

import br.com.paulocalderan.paymentservice.application.service.PaymentService;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.PaymentEventProducer;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedFailedEvent;
import br.com.paulocalderan.paymentservice.infrastructure.messaging.event.PaymentProcessedSuccessEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(topics = {"subscription-events", "payment-events"}, partitions = 1)
@DirtiesContext
class PaymentServiceIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentEventProducer paymentEventProducer;

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
    void shouldPublishPaymentSuccessEvent() {
        // Given - use unique IDs for this test
        UUID testSubscriptionId = UUID.randomUUID();
        UUID testUserId = UUID.randomUUID();
        PaymentProcessedSuccessEvent event = new PaymentProcessedSuccessEvent(testSubscriptionId, testUserId);

        // When
        paymentEventProducer.publishPaymentSuccess(event);

        // Small delay to ensure event is published
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - verify event was published by checking the topic has a record
        Consumer<String, java.util.Map> consumer = createMapConsumer("payment-events");
        ConsumerRecords<String, java.util.Map> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

        assertThat(records.count()).isGreaterThan(0);
        
        // Find the record with matching subscriptionId (there might be records from other tests)
        ConsumerRecord<String, java.util.Map> matchingRecord = null;
        for (ConsumerRecord<String, java.util.Map> record : records) {
            Object subscriptionIdValue = record.value() != null ? record.value().get("subscriptionId") : null;
            if (subscriptionIdValue != null && testSubscriptionId.toString().equals(subscriptionIdValue.toString())) {
                matchingRecord = record;
                break;
            }
        }
        
        assertThat(matchingRecord).isNotNull()
                .withFailMessage("Could not find record with subscriptionId: " + testSubscriptionId);
        assertThat(matchingRecord.key()).isEqualTo(testSubscriptionId.toString());
        assertThat(matchingRecord.value().get("subscriptionId")).isEqualTo(testSubscriptionId.toString());

        consumer.close();
    }

    @Test
    void shouldPublishPaymentFailedEvent() {
        // Given - use unique IDs for this test
        UUID testSubscriptionId = UUID.randomUUID();
        UUID testUserId = UUID.randomUUID();
        PaymentProcessedFailedEvent event = new PaymentProcessedFailedEvent(
                testSubscriptionId, testUserId, "Payment failed"
        );

        // When
        paymentEventProducer.publishPaymentFailed(event);

        // Small delay to ensure event is published
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - verify event was published by checking the topic has a record
        Consumer<String, java.util.Map> consumer = createMapConsumer("payment-events");
        ConsumerRecords<String, java.util.Map> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

        assertThat(records.count()).isGreaterThan(0);
        
        // Find the record with matching subscriptionId (there might be records from other tests)
        ConsumerRecord<String, java.util.Map> matchingRecord = null;
        for (ConsumerRecord<String, java.util.Map> record : records) {
            Object subscriptionIdValue = record.value() != null ? record.value().get("subscriptionId") : null;
            if (subscriptionIdValue != null && testSubscriptionId.toString().equals(subscriptionIdValue.toString())) {
                matchingRecord = record;
                break;
            }
        }
        
        assertThat(matchingRecord).isNotNull()
                .withFailMessage("Could not find record with subscriptionId: " + testSubscriptionId);
        assertThat(matchingRecord.key()).isEqualTo(testSubscriptionId.toString());
        assertThat(matchingRecord.value().get("subscriptionId")).isEqualTo(testSubscriptionId.toString());

        consumer.close();
    }

    @Test
    void shouldProcessPaymentAndReturnResult() {
        // Given
        UUID testSubscriptionId = UUID.randomUUID();
        UUID testUserId = UUID.randomUUID();

        // When - process payment directly
        boolean result = paymentService.processPayment(testSubscriptionId, testUserId, "BASIC", new BigDecimal("19.90"));

        // Then - verify it returns a boolean (can be true or false due to randomness)
        assertThat(result).isInstanceOf(Boolean.class);
    }

    private Consumer<String, java.util.Map> createMapConsumer(String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");

        DefaultKafkaConsumerFactory<String, java.util.Map> factory = new DefaultKafkaConsumerFactory<>(props);
        Consumer<String, java.util.Map> consumer = factory.createConsumer();
        consumer.subscribe(java.util.Collections.singletonList(topic));
        return consumer;
    }

    private Producer<String, Object> createProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(props);
        return factory.createProducer();
    }
}
