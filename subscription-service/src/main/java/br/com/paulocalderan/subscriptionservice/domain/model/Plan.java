package br.com.paulocalderan.subscriptionservice.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum Plan {
    BASIC("BASIC", new BigDecimal("19.90")),
    PREMIUM("PREMIUM", new BigDecimal("39.90")),
    FAMILY("FAMILY", new BigDecimal("59.90"));

    private final String name;
    private final BigDecimal value;

    public static Plan fromString(String name) {
        for (Plan plan : values()) {
            if (plan.name.equalsIgnoreCase(name) || plan.name().equalsIgnoreCase(name)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Invalid plan: " + name);
    }
}

