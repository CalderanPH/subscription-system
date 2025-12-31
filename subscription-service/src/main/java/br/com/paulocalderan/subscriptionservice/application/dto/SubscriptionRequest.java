package br.com.paulocalderan.subscriptionservice.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SubscriptionRequest(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotNull(message = "Plan is required")
    String plan
) {}

