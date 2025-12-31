package br.com.paulocalderan.subscriptionservice.application.mapper;

import br.com.paulocalderan.subscriptionservice.application.dto.SubscriptionRequest;
import br.com.paulocalderan.subscriptionservice.application.dto.SubscriptionResponse;
import br.com.paulocalderan.subscriptionservice.domain.model.Plan;
import br.com.paulocalderan.subscriptionservice.domain.model.Status;
import br.com.paulocalderan.subscriptionservice.domain.model.Subscription;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SubscriptionMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "plan", expression = "java(parsePlan(request.plan()))")
    @Mapping(target = "startDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "expirationDate", expression = "java(java.time.LocalDate.now().plusMonths(1))")
    @Mapping(target = "status", constant = "PROCESSING")
    @Mapping(target = "renewalAttempts", constant = "0")
    @Mapping(target = "version", ignore = true)
    Subscription toEntity(SubscriptionRequest request);

    @Mapping(target = "plan", expression = "java(subscription.getPlan().name())")
    @Mapping(target = "status", expression = "java(subscription.getStatus().name())")
    SubscriptionResponse toResponse(Subscription subscription);

    default Plan parsePlan(String plan) {
        return Plan.fromString(plan);
    }
}

