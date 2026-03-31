package com.ecommerce.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Auth Service API")
                .version("1.0.0")
                .description("Authentication and Authorization service. Register users, login to get JWT tokens, and refresh expired tokens. Supports role-based access: CUSTOMER, ADMIN, SELLER.")
                .contact(new Contact().name("E-Commerce Platform").email("developer@ecommerce.com"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
