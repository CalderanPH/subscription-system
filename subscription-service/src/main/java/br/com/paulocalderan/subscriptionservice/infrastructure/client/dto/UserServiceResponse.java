package br.com.paulocalderan.subscriptionservice.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserServiceResponse<T>(
    Boolean success,
    String message,
    T data
) {}

