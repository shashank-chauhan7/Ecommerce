package com.ecommerce.loadtest.scenarios;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class AuthLoadSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/AuthLoad");

    private final Iterator<Map<String, Object>> userFeeder = Stream.generate(() -> {
        Map<String, Object> map = new HashMap<>();
        String id = UUID.randomUUID().toString().substring(0, 8);
        map.put("email", "auth_" + id + "@loadtest.com");
        map.put("password", "SecurePass123!");
        map.put("firstName", "Auth");
        map.put("lastName", "Tester");
        return map;
    }).iterator();

    private final ScenarioBuilder authScenario = scenario("Auth Load Test")
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
                            .check(jsonPath("$.data.refreshToken").saveAs("refreshToken"))
            )
            .pause(Duration.ofMillis(200), Duration.ofSeconds(1))
            .exec(
                    http("Login User")
                            .post("/api/auth/login")
                            .body(StringBody("""
                                    {
                                        "email": "#{email}",
                                        "password": "#{password}"
                                    }
                                    """))
                            .check(status().is(200))
                            .check(jsonPath("$.data.accessToken").saveAs("loginAccessToken"))
                            .check(jsonPath("$.data.refreshToken").saveAs("loginRefreshToken"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .exec(
                    http("Refresh Token")
                            .post("/api/auth/refresh")
                            .body(StringBody("""
                                    {
                                        "refreshToken": "#{loginRefreshToken}"
                                    }
                                    """))
                            .check(status().is(200))
                            .check(jsonPath("$.data.accessToken").exists())
            );

    {
        setUp(
                authScenario.injectOpen(
                        rampUsers(500).during(Duration.ofSeconds(30))
                )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().percentile(99.0).lt(1000),
                        global().successfulRequests().percent().gt(99.0)
                );
    }
}
