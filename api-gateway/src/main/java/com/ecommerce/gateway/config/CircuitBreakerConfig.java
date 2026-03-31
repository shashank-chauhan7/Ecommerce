package com.ecommerce.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CircuitBreakerConfig {

    private static final List<String> CIRCUIT_BREAKER_NAMES = List.of(
            "authServiceCB",
            "userServiceCB",
            "productServiceCB",
            "inventoryServiceCB",
            "orderServiceCB",
            "paymentServiceCB",
            "searchServiceCB",
            "recommendationServiceCB"
    );

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig defaultConfig =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .slidingWindowSize(10)
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        CIRCUIT_BREAKER_NAMES.forEach(registry::circuitBreaker);

        return registry;
    }
}
