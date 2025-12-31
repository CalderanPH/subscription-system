package br.com.paulocalderan.subscriptionservice.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubscriptionMetrics {

    private final MeterRegistry meterRegistry;

    public SubscriptionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementSubscriptionCreated(String plan) {
        Counter counter = Counter.builder("subscription_created_total")
                .description("Total number of subscriptions created by plan")
                .tags(Tags.of("service", "subscription-service", "plan", plan))
                .register(meterRegistry);
        counter.increment();
        log.info("Metric incremented: subscription_created_total, plan: {}, count: {}", plan, counter.count());
    }

    public void incrementSubscriptionCancelled(String plan) {
        Counter.builder("subscription_cancelled_total")
                .description("Total number of subscriptions cancelled by plan")
                .tags(Tags.of("service", "subscription-service", "plan", plan))
                .register(meterRegistry)
                .increment();
    }

    public void incrementSubscriptionRenewed(String plan) {
        Counter.builder("subscription_renewed_total")
                .description("Total number of subscriptions renewed by plan")
                .tags(Tags.of("service", "subscription-service", "plan", plan))
                .register(meterRegistry)
                .increment();
    }

    public void incrementRenewalError(String plan) {
        Counter.builder("subscription_renewal_error_total")
                .description("Total number of subscription renewal errors by plan")
                .tags(Tags.of("service", "subscription-service", "plan", plan))
                .register(meterRegistry)
                .increment();
    }

    public void incrementSubscriptionFailed(String plan) {
        Counter.builder("subscription_failed_total")
                .description("Total number of subscriptions that failed payment by plan")
                .tags(Tags.of("service", "subscription-service", "plan", plan))
                .register(meterRegistry)
                .increment();
    }
}

