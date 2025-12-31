package br.com.paulocalderan.subscriptionservice.application.service;

import br.com.paulocalderan.subscriptionservice.application.dto.SubscriptionRequest;
import br.com.paulocalderan.subscriptionservice.application.dto.SubscriptionResponse;
import br.com.paulocalderan.subscriptionservice.application.mapper.SubscriptionMapper;
import br.com.paulocalderan.subscriptionservice.common.exception.DuplicateSubscriptionException;
import br.com.paulocalderan.subscriptionservice.common.exception.SubscriptionAlreadyCancelledException;
import br.com.paulocalderan.subscriptionservice.common.exception.SubscriptionNotFoundException;
import br.com.paulocalderan.subscriptionservice.domain.model.Status;
import br.com.paulocalderan.subscriptionservice.domain.model.Subscription;
import br.com.paulocalderan.subscriptionservice.domain.repository.SubscriptionRepository;
import br.com.paulocalderan.subscriptionservice.common.exception.UserNotFoundException;
import br.com.paulocalderan.subscriptionservice.infrastructure.cache.SubscriptionCacheService;
import br.com.paulocalderan.subscriptionservice.infrastructure.client.UserServiceClient;
import br.com.paulocalderan.subscriptionservice.infrastructure.client.dto.UserResponse;
import br.com.paulocalderan.subscriptionservice.infrastructure.client.dto.UserServiceResponse;
import br.com.paulocalderan.subscriptionservice.infrastructure.messaging.SubscriptionEventProducer;
import br.com.paulocalderan.subscriptionservice.infrastructure.messaging.event.*;
import br.com.paulocalderan.subscriptionservice.infrastructure.metrics.SubscriptionMetrics;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionCacheService cacheService;
    private final SubscriptionEventProducer eventProducer;
    private final SubscriptionMetrics metrics;
    private final UserServiceClient userServiceClient;

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        log.info("Creating subscription for user: {}", request.userId());

        validateUserExists(request.userId());

        if (subscriptionRepository.existsByUserIdAndStatus(request.userId(), Status.ACTIVE) ||
            subscriptionRepository.existsByUserIdAndStatus(request.userId(), Status.PROCESSING)) {
            throw new DuplicateSubscriptionException(
                    "User already has an active or processing subscription: " + request.userId());
        }

        Subscription subscription = subscriptionMapper.toEntity(request);
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        cacheService.invalidateByUserId(request.userId());

        eventProducer.publishSubscriptionCreated(
                new SubscriptionCreatedEvent(
                        savedSubscription.getId(),
                        savedSubscription.getUserId(),
                        savedSubscription.getPlan().name(),
                        savedSubscription.getPlan().getValue()));

        log.info("Subscription created with PROCESSING status, waiting for payment confirmation: {}", savedSubscription.getId());
        return subscriptionMapper.toResponse(savedSubscription);
    }

    public SubscriptionResponse findById(UUID id) {
        log.info("Finding subscription by id: {}", id);
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));
        return subscriptionMapper.toResponse(subscription);
    }

    public SubscriptionResponse findActiveByUserId(UUID userId) {
        log.info("Finding active subscription for user: {}", userId);
        Subscription subscription = cacheService.findActiveByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Active subscription not found for user: " + userId));
        return subscriptionMapper.toResponse(subscription);
    }

    public List<SubscriptionResponse> findAll() {
        log.info("Finding all subscriptions");
        return subscriptionRepository.findAll().stream()
                .map(subscriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubscriptionResponse cancel(UUID id) {
        log.info("Cancelling subscription with id: {}", id);
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));

        if (subscription.getStatus() == Status.CANCELLED) {
            throw new SubscriptionAlreadyCancelledException("Subscription is already cancelled: " + id);
        }

        subscription.cancel();
        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        cacheService.invalidateByUserId(subscription.getUserId());

        metrics.incrementSubscriptionCancelled(updatedSubscription.getPlan().name());

        eventProducer.publishSubscriptionCancelled(
                new SubscriptionCancelledEvent(
                        updatedSubscription.getId(),
                        updatedSubscription.getUserId()));

        log.info("Subscription cancelled successfully with id: {}", id);
        return subscriptionMapper.toResponse(updatedSubscription);
    }

    @Transactional
    public Subscription renewSubscription(UUID id) {
        log.info("Renewing subscription with id: {}", id);
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));

        subscription.renew();
        Subscription renewedSubscription = subscriptionRepository.save(subscription);

        cacheService.invalidateByUserId(subscription.getUserId());

        metrics.incrementSubscriptionRenewed(renewedSubscription.getPlan().name());

        log.info("Subscription renewed successfully with id: {}", id);
        return renewedSubscription;
    }

    @Transactional
    public Subscription incrementFailedAttempt(UUID id) {
        log.info("Incrementing failed attempt for subscription with id: {}", id);
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));

        subscription.incrementFailedAttempt();
        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        cacheService.invalidateByUserId(subscription.getUserId());

        log.info("Failed attempt incremented for subscription with id: {}", id);
        return updatedSubscription;
    }

    @Transactional
    public Subscription activateSubscription(UUID id) {
        log.info("Activating subscription with id: {}", id);
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));

        subscription.activate();
        Subscription activatedSubscription = subscriptionRepository.save(subscription);

        cacheService.invalidateByUserId(subscription.getUserId());

        metrics.incrementSubscriptionCreated(activatedSubscription.getPlan().name());

        log.info("Subscription activated successfully with id: {}", id);
        return activatedSubscription;
    }

    @Transactional
    public Subscription failSubscription(UUID id) {
        log.info("Marking subscription as failed with id: {}", id);
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));

        subscription.fail();
        Subscription failedSubscription = subscriptionRepository.save(subscription);

        cacheService.invalidateByUserId(subscription.getUserId());

        metrics.incrementSubscriptionFailed(failedSubscription.getPlan().name());

        log.info("Subscription marked as failed with id: {}", id);
        return failedSubscription;
    }

    public List<Subscription> findExpiringOnDate(java.time.LocalDate date) {
        log.info("Finding subscriptions expiring on or before date: {}", date);
        return subscriptionRepository.findExpiredOrExpiringOnDate(date);
    }

    @Transactional
    public void cancelStaleProcessingSubscription(UUID id) {
        log.info("Cancelling stale processing subscription with id: {}", id);
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));

        if (subscription.getStatus() != Status.PROCESSING) {
            log.warn("Subscription {} is not in PROCESSING status, cannot cancel as stale. Current status: {}", 
                    id, subscription.getStatus());
            return;
        }

        subscription.cancel();
        subscription.setExpirationDate(java.time.LocalDate.now());
        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        cacheService.invalidateByUserId(subscription.getUserId());

        eventProducer.publishSubscriptionCancelled(
                new SubscriptionCancelledEvent(
                        updatedSubscription.getId(),
                        updatedSubscription.getUserId()));

        log.info("Stale processing subscription cancelled successfully with id: {} (expirationDate set to today)", id);
    }

    private void validateUserExists(UUID userId) {
        try {
            ResponseEntity<UserServiceResponse<UserResponse>> response = userServiceClient.findById(userId);
            
            if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                throw new UserNotFoundException("User not found with id: " + userId);
            }
            
            UserServiceResponse<UserResponse> responseBody = response.getBody();
            if (responseBody == null || responseBody.data() == null) {
                throw new UserNotFoundException("User not found with id: " + userId);
            }
            
            log.debug("User validation successful for user: {}", userId);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            log.warn("User not found via Feign: {}", userId);
            throw new UserNotFoundException("User not found with id: " + userId);
        } catch (FeignException e) {
            log.error("Error calling User Service for user: {}, status: {}", userId, e.status(), e);
            throw new UserNotFoundException("User not found with id: " + userId);
        } catch (Exception e) {
            log.error("Unexpected error validating user: {}", userId, e);
            throw new UserNotFoundException("User not found with id: " + userId);
        }
    }
}

