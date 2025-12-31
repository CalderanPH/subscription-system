package br.com.paulocalderan.subscriptionservice.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(
    UUID id,
    String name,
    String email,
    String cpf,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

