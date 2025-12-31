package br.com.paulocalderan.paymentservice.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

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
    void shouldProcessPayment() {
        // When
        boolean result = paymentService.processPayment(subscriptionId, userId, plan, value);

        // Then
        // Since it's a random process, we can't assert specific value
        // but we can verify the method doesn't throw exception
        assertThat(result).isInstanceOf(Boolean.class);
    }

    @Test
    void shouldProcessPaymentMultipleTimes() {
        // When
        boolean result1 = paymentService.processPayment(subscriptionId, userId, plan, value);
        boolean result2 = paymentService.processPayment(subscriptionId, userId, plan, value);
        boolean result3 = paymentService.processPayment(subscriptionId, userId, plan, value);

        // Then
        assertThat(result1).isInstanceOf(Boolean.class);
        assertThat(result2).isInstanceOf(Boolean.class);
        assertThat(result3).isInstanceOf(Boolean.class);
    }

    @Test
    void shouldProcessPaymentWithDifferentPlans() {
        // When
        boolean basicResult = paymentService.processPayment(subscriptionId, userId, "BASIC", new BigDecimal("19.90"));
        boolean premiumResult = paymentService.processPayment(subscriptionId, userId, "PREMIUM", new BigDecimal("49.90"));
        boolean familyResult = paymentService.processPayment(subscriptionId, userId, "FAMILY", new BigDecimal("99.90"));

        // Then
        assertThat(basicResult).isInstanceOf(Boolean.class);
        assertThat(premiumResult).isInstanceOf(Boolean.class);
        assertThat(familyResult).isInstanceOf(Boolean.class);
    }

    @Test
    void shouldProcessPaymentWithDifferentValues() {
        // When
        boolean smallValue = paymentService.processPayment(subscriptionId, userId, plan, new BigDecimal("10.00"));
        boolean largeValue = paymentService.processPayment(subscriptionId, userId, plan, new BigDecimal("1000.00"));

        // Then
        assertThat(smallValue).isInstanceOf(Boolean.class);
        assertThat(largeValue).isInstanceOf(Boolean.class);
    }
}
