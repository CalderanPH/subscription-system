package br.com.paulocalderan.paymentservice.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ErrorResponse(
    String message,
    String error,
    int status,
    String path,
    LocalDateTime timestamp,
    List<ValidationError> validationErrors
) {
    @Builder
    public record ValidationError(
        String field,
        String message
    ) {}
}

