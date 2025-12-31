package br.com.paulocalderan.subscriptionservice.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID userId,
    String plan,
    LocalDate startDate,
    LocalDate expirationDate,
    String status,
    Integer renewalAttempts
) {}

