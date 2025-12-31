package br.com.paulocalderan.subscriptionservice.infrastructure.client;

import br.com.paulocalderan.subscriptionservice.infrastructure.client.dto.UserResponse;
import br.com.paulocalderan.subscriptionservice.infrastructure.client.dto.UserServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Slf4j
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public ResponseEntity<UserServiceResponse<UserResponse>> findById(UUID id) {
        log.warn("User Service unavailable, fallback called for user: {}", id);
        return ResponseEntity.notFound().build();
    }
}

