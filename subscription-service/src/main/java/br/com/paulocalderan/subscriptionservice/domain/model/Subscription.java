package br.com.paulocalderan.subscriptionservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.UUID;

@Document(collection = "subscriptions")
@CompoundIndex(
    name = "active_subscription_per_user",
    def = "{'userId': 1, 'status': 1}",
    unique = true,
    partialFilter = "{'status': 'ACTIVE'}"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    private UUID id;

    private UUID userId;

    private Plan plan;

    private LocalDate startDate;

    private LocalDate expirationDate;

    private Status status;

    @Builder.Default
    private Integer renewalAttempts = 0;

    @Version
    private Long version;

    public void renew() {
        if (this.status != Status.ACTIVE) {
            throw new IllegalStateException("Only active subscriptions can be renewed");
        }
        this.startDate = LocalDate.now();
        this.expirationDate = calculateNewExpirationDate();
        this.renewalAttempts = 0;
    }

    public void incrementFailedAttempt() {
        this.renewalAttempts++;
        if (this.renewalAttempts >= 3) {
            this.status = Status.SUSPENDED;
        }
    }

    public void cancel() {
        if (this.status == Status.CANCELLED) {
            throw new IllegalStateException("Subscription is already cancelled");
        }
        this.status = Status.CANCELLED;
    }

    public void activate() {
        if (this.status != Status.PROCESSING) {
            throw new IllegalStateException("Only processing subscriptions can be activated");
        }
        this.status = Status.ACTIVE;
    }

    public void fail() {
        if (this.status != Status.PROCESSING) {
            throw new IllegalStateException("Only processing subscriptions can be marked as failed");
        }
        this.status = Status.FAILED;
    }

    public boolean isActive() {
        return status == Status.ACTIVE && 
               LocalDate.now().isBefore(expirationDate.plusDays(1));
    }

    private LocalDate calculateNewExpirationDate() {
        return LocalDate.now().plusMonths(1);
    }
}

