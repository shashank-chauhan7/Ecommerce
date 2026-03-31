package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // --- OpenAPI doc routes (no auth, no rate limit) ---
                .route("auth-service-docs", r -> r.path("/service-docs/auth/**")
                        .filters(f -> f.rewritePath("/service-docs/auth/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://auth-service"))
                .route("user-service-docs", r -> r.path("/service-docs/user/**")
                        .filters(f -> f.rewritePath("/service-docs/user/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://user-service"))
                .route("product-service-docs", r -> r.path("/service-docs/product/**")
                        .filters(f -> f.rewritePath("/service-docs/product/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://product-service"))
                .route("inventory-service-docs", r -> r.path("/service-docs/inventory/**")
                        .filters(f -> f.rewritePath("/service-docs/inventory/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://inventory-service"))
                .route("order-service-docs", r -> r.path("/service-docs/order/**")
                        .filters(f -> f.rewritePath("/service-docs/order/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://order-service"))
                .route("payment-service-docs", r -> r.path("/service-docs/payment/**")
                        .filters(f -> f.rewritePath("/service-docs/payment/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://payment-service"))
                .route("search-service-docs", r -> r.path("/service-docs/search/**")
                        .filters(f -> f.rewritePath("/service-docs/search/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://search-service"))
                .route("notification-service-docs", r -> r.path("/service-docs/notification/**")
                        .filters(f -> f.rewritePath("/service-docs/notification/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://notification-service"))
                .route("ai-service-docs", r -> r.path("/service-docs/ai/**")
                        .filters(f -> f.rewritePath("/service-docs/ai/(?<remaining>.*)", "/${remaining}"))
                        .uri("lb://ai-recommendation-service"))

                // --- Business routes (with circuit breaker + rate limiter) ---
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("authServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://auth-service"))

                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("userServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://user-service"))

                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("productServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://product-service"))

                .route("inventory-service", r -> r.path("/api/inventory/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("inventoryServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://inventory-service"))

                .route("order-service", r -> r.path("/api/orders/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("orderServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://order-service"))

                .route("payment-service", r -> r.path("/api/payments/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("paymentServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://payment-service"))

                .route("notification-service", r -> r.path("/api/notifications/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("notificationServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://notification-service"))

                .route("search-service", r -> r.path("/api/search/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("searchServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://search-service"))

                .route("ai-recommendation-service", r -> r.path("/api/recommendations/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb.setName("recommendationServiceCB").setFallbackUri("forward:/fallback")))
                        .uri("lb://ai-recommendation-service"))

                .build();
    }
}
