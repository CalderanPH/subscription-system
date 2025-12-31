package br.com.paulocalderan.subscriptionservice.infrastructure.client;

import br.com.paulocalderan.subscriptionservice.infrastructure.client.dto.UserResponse;
import br.com.paulocalderan.subscriptionservice.infrastructure.client.dto.UserServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/users", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/{id}")
    ResponseEntity<UserServiceResponse<UserResponse>> findById(@PathVariable UUID id);
}

