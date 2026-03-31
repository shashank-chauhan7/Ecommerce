package com.ecommerce.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Service API")
                .version("1.0.0")
                .description("Order lifecycle management using the SAGA choreography pattern. Orders flow through states: CREATED \u2192 PENDING \u2192 INVENTORY_RESERVED \u2192 PAYMENT_PROCESSING \u2192 CONFIRMED \u2192 SHIPPED \u2192 DELIVERED. Supports cancellation with compensating transactions.")
                .contact(new Contact().name("E-Commerce Platform").email("developer@ecommerce.com"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter your JWT token. Get one from Auth Service /api/auth/login")));
    }
}
