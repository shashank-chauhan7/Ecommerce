package com.ecommerce.loadtest.scenarios;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class FlashSaleSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/FlashSale");

    private final Iterator<Map<String, Object>> productIdFeeder = Stream.generate(() -> {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", UUID.randomUUID().toString());
        return map;
    }).iterator();

    private final Iterator<Map<String, Object>> userFeeder = Stream.generate(() -> {
        Map<String, Object> map = new HashMap<>();
        String id = UUID.randomUUID().toString().substring(0, 8);
        map.put("email", "user_" + id + "@loadtest.com");
        map.put("password", "LoadTest123!");
        map.put("firstName", "Load");
        map.put("lastName", "Tester");
        return map;
    }).iterator();

    private final ScenarioBuilder flashSaleScenario = scenario("Flash Sale Day")
            .feed(userFeeder)
            .exec(
                    http("Register User")
                            .post("/api/auth/register")
                            .body(StringBody("""
                                    {
                                        "email": "#{email}",
                                        "password": "#{password}",
                                        "firstName": "#{firstName}",
                                        "lastName": "#{lastName}"
                                    }
                                    """))
                            .check(status().is(201))
                            .check(jsonPath("$.data.accessToken").saveAs("accessToken"))
            )
            .pause(Duration.ofMillis(500), Duration.ofSeconds(2))
            .exec(
                    http("Browse Products")
                            .get("/api/products?page=0&size=20")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1), Duration.ofSeconds(3))
            .exec(
                    http("Search Products")
                            .get("/api/search?query=electronics")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().in(200, 404))
            )
            .pause(Duration.ofMillis(500), Duration.ofSeconds(2))
            .feed(productIdFeeder)
            .exec(
                    http("View Product Detail")
                            .get("/api/products/#{productId}")
                            .header("Authorization", "Bearer #{accessToken}")
                            .check(status().in(200, 404))
            )
            .pause(Duration.ofSeconds(1), Duration.ofSeconds(3))
            .exec(session -> {
                String productId = UUID.randomUUID().toString();
                int quantity = ThreadLocalRandom.current().nextInt(1, 5);
                return session
                        .set("orderProductId", productId)
                        .set("orderQuantity", quantity);
            })
            .exec(
                    http("Place Order")
                            .post("/api/orders")
                            .header("Authorization", "Bearer #{accessToken}")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .body(StringBody("""
                                    {
                                        "items": [{
                                            "productId": "#{orderProductId}",
                                            "productName": "Flash Sale Item",
                                            "quantity": #{orderQuantity},
                                            "unitPrice": 29.99,
                                            "totalPrice": 29.99
                                        }],
                                        "shippingAddress": "123 Load Test Ave, Test City, TC 12345"
                                    }
                                    """))
                            .check(status().in(200, 201, 400, 404))
                            .check(jsonPath("$.data.id").optional().saveAs("orderId"))
            )
            .pause(Duration.ofSeconds(1), Duration.ofSeconds(2))
            .doIf(session -> session.contains("orderId")).then(
                    exec(
                            http("Check Order Status")
                                    .get("/api/orders/#{orderId}")
                                    .header("Authorization", "Bearer #{accessToken}")
                                    .check(status().in(200, 404))
                    )
            );

    {
        setUp(
                flashSaleScenario.injectOpen(
                        rampUsers(1000).during(Duration.ofSeconds(60))
                )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().max().lt(3000),
                        global().successfulRequests().percent().gt(95.0)
                );
    }
}
