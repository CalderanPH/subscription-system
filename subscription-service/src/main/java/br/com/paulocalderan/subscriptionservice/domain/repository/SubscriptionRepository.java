package br.com.paulocalderan.subscriptionservice.domain.repository;

import br.com.paulocalderan.subscriptionservice.domain.model.Status;
import br.com.paulocalderan.subscriptionservice.domain.model.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends MongoRepository<Subscription, UUID> {

    Optional<Subscription> findByUserIdAndStatus(UUID userId, Status status);

    List<Subscription> findByStatus(Status status);

    @Query("{'expirationDate': ?0, 'status': 'ACTIVE'}")
    List<Subscription> findExpiringOnDate(LocalDate date);

    @Query("{'expirationDate': {$lte: ?0}, 'status': 'ACTIVE'}")
    List<Subscription> findExpiredOrExpiringOnDate(LocalDate date);

    boolean existsByUserIdAndStatus(UUID userId, Status status);
}

