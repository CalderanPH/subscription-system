package br.com.paulocalderan.subscriptionservice.infrastructure.cache;

import br.com.paulocalderan.subscriptionservice.domain.model.Status;
import br.com.paulocalderan.subscriptionservice.domain.model.Subscription;
import br.com.paulocalderan.subscriptionservice.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionCacheService {

    private final RedisTemplate<String, Subscription> redisTemplate;
    private final SubscriptionRepository subscriptionRepository;

    private static final String CACHE_KEY_PREFIX = "subscription:active:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    public Optional<Subscription> findActiveByUserId(UUID userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        try {
            Subscription cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit for user: {}", userId);
                return Optional.of(cached);
            }

            log.debug("Cache miss for user: {}, fetching from database", userId);
            Optional<Subscription> subscription = subscriptionRepository
                    .findByUserIdAndStatus(userId, Status.ACTIVE);

            if (subscription.isPresent()) {
                redisTemplate.opsForValue().set(cacheKey, subscription.get(), CACHE_TTL);
                log.debug("Cached subscription for user: {}", userId);
            }

            return subscription;
        } catch (Exception e) {
            log.error("Error accessing cache for user: {}", userId, e);
            return subscriptionRepository.findByUserIdAndStatus(userId, Status.ACTIVE);
        }
    }

    public void invalidateByUserId(UUID userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        try {
            redisTemplate.delete(cacheKey);
            log.debug("Cache invalidated for user: {}", userId);
        } catch (Exception e) {
            log.error("Error invalidating cache for user: {}", userId, e);
        }
    }

    public void invalidateBySubscriptionId(UUID subscriptionId) {
        subscriptionRepository.findById(subscriptionId)
                .ifPresent(subscription -> invalidateByUserId(subscription.getUserId()));
    }
}

