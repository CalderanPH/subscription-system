package br.com.paulocalderan.subscriptionservice.infrastructure.scheduler;

import br.com.paulocalderan.subscriptionservice.application.service.SubscriptionService;
import br.com.paulocalderan.subscriptionservice.domain.model.Status;
import br.com.paulocalderan.subscriptionservice.domain.model.Subscription;
import br.com.paulocalderan.subscriptionservice.domain.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class ProcessStaleProcessingSubscriptionsJob implements Job {

    private final ApplicationContext applicationContext;

    private static final String LOCK_KEY = "stale-processing-subscriptions:lock";
    private static final Duration LOCK_WAIT_TIME = Duration.ofSeconds(5);
    private static final Duration LOCK_LEASE_TIME = Duration.ofMinutes(5);

    public ProcessStaleProcessingSubscriptionsJob(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SubscriptionRepository subscriptionRepository = applicationContext.getBean(SubscriptionRepository.class);
        SubscriptionService subscriptionService = applicationContext.getBean(SubscriptionService.class);
        RedissonClient redissonClient = applicationContext.getBean(RedissonClient.class);

        log.info("Starting stale processing subscriptions cleanup job");

        RLock lock = redissonClient.getLock(LOCK_KEY);

        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME.toMillis(), LOCK_LEASE_TIME.toMillis(), 
                    java.util.concurrent.TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("Could not acquire lock for stale processing subscriptions job (possibly handled by another instance)");
                return;
            }

            try {
                List<Subscription> processingSubscriptions = subscriptionRepository.findByStatus(Status.PROCESSING);
                
                log.info("Found {} subscriptions in PROCESSING status", processingSubscriptions.size());

                LocalDate yesterday = LocalDate.now().minusDays(1);
                int cancelledCount = 0;

                for (Subscription subscription : processingSubscriptions) {
                    if (subscription.getStartDate() == null || 
                        subscription.getStartDate().isBefore(yesterday) ||
                        subscription.getStartDate().isEqual(yesterday)) {
                        
                        try {
                            log.info("Cancelling stale processing subscription: {} (startDate: {})", 
                                    subscription.getId(), subscription.getStartDate());
                            subscriptionService.cancelStaleProcessingSubscription(subscription.getId());
                            cancelledCount++;
                        } catch (Exception e) {
                            log.error("Error cancelling stale processing subscription: {}", 
                                    subscription.getId(), e);
                        }
                    }
                }

                log.info("Stale processing subscriptions cleanup job completed. Cancelled {} subscriptions", cancelledCount);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("Lock released for stale processing subscriptions job");
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while trying to acquire lock for stale processing subscriptions job", e);
        } catch (Exception e) {
            log.error("Error executing stale processing subscriptions cleanup job", e);
            throw new JobExecutionException("Failed to execute stale processing subscriptions cleanup job", e);
        }
    }
}

