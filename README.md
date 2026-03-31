# E-Commerce Platform

[![Build Status](https://img.shields.io/github/actions/workflow/status/your-org/ecommerce/ci.yml?branch=main&style=flat-square)](https://github.com/your-org/ecommerce/actions)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

A **production-grade**, microservices-based E-Commerce platform built with **Spring Boot 3.4.4** and **Java 21**. The system demonstrates enterprise patterns including **Event-Driven Architecture**, **SAGA orchestration**, **CQRS**, **Circuit Breakers**, and **AI-powered recommendations** — all deployed with Docker and Kubernetes.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Microservices](#microservices)
- [Design Patterns](#design-patterns)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [SAGA Order Flow](#saga-order-flow)
- [Testing](#testing)
- [Deployment](#deployment)
- [CI/CD](#cicd)
- [Monitoring & Observability](#monitoring--observability)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

---

## Architecture Overview

```
                            ┌──────────────────┐
                            │   API Gateway    │ :8080
                            │  (Spring Cloud)  │
                            └────────┬─────────┘
                                     │
                 ┌───────────────────┼───────────────────┐
                 │                   │                   │
          ┌──────┴──────┐   ┌───────┴──────┐   ┌───────┴──────┐
          │ Auth Service│   │ User Service │   │Product Svc   │
          │    :8081    │   │    :8082     │   │    :8083     │
          └─────────────┘   └──────────────┘   └──────────────┘
                 │
    ┌────────────┼──────────────┬──────────────┐
    │            │              │              │
┌───┴────┐ ┌────┴─────┐ ┌─────┴────┐ ┌───────┴──────┐
│Inventory│ │  Order   │ │ Payment  │ │Notification  │
│ :8084   │ │  :8085   │ │  :8086   │ │   :8087      │
└────┬────┘ └────┬─────┘ └────┬─────┘ └──────────────┘
     │           │            │
     └───────────┴────────────┘
                 │
          ┌──────┴──────┐
          │ Apache Kafka│  (Event Bus)
          └─────────────┘

    ┌──────────────┐   ┌───────────────────┐
    │Search Service│   │ AI Recommendation │
    │    :8088     │   │     :8089         │
    └──────────────┘   └───────────────────┘
```

The platform follows a **microservices architecture** with:
- **API Gateway** as the single entry point with rate limiting, JWT validation, and request routing
- **Event-Driven Communication** via Apache Kafka for asynchronous, decoupled inter-service messaging
- **SAGA Pattern** for distributed transaction management across Order → Payment → Inventory
- **Config Server** for centralized, externalized configuration management
- **Circuit Breakers** (Resilience4j) for fault tolerance and graceful degradation

---

## Microservices

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| **Config Server** | `8888` | — | Centralized configuration management for all services |
| **API Gateway** | `8080` | Redis | Single entry point — routing, rate limiting, JWT validation |
| **Auth Service** | `8081` | PostgreSQL | Authentication & authorization — JWT token issuance, refresh, RBAC |
| **User Service** | `8082` | PostgreSQL, Redis | User profile management, addresses, preferences |
| **Product Service** | `8083` | PostgreSQL, Redis | Product catalog, categories, S3 image management |
| **Inventory Service** | `8084` | Cassandra, Redis | Real-time stock management with distributed locking |
| **Order Service** | `8085` | PostgreSQL, Kafka | Order lifecycle, SAGA orchestration, state machine |
| **Payment Service** | `8086` | PostgreSQL, Kafka | Payment processing (Stripe), refunds, transaction history |
| **Notification Service** | `8087` | Kafka | Email notifications via Thymeleaf templates, event-driven |
| **Search Service** | `8088` | Elasticsearch, Redis | Full-text product search with faceted filtering |
| **AI Recommendation** | `8089` | Redis, OpenAI | AI-powered product recommendations using Spring AI |

---

## Design Patterns

### Creational Patterns
| Pattern | Where Used |
|---------|------------|
| **Builder** | DTOs, Order/Payment entities, API responses across all services |
| **Factory Method** | `PaymentProcessorFactory` (Stripe/Mock), `NotificationFactory` (Email/SMS/Push) |
| **Singleton** | Spring-managed beans (`@Service`, `@Component`, `@Configuration`) |

### Structural Patterns
| Pattern | Where Used |
|---------|------------|
| **Facade** | Service layer classes abstracting complex business logic |
| **Adapter** | Payment gateway adapters (Stripe adapter), search engine adapter |
| **Decorator** | Redis caching decorators on service methods (`@Cacheable`) |
| **Proxy** | Spring AOP proxies for `@Transactional`, `@CircuitBreaker`, `@RateLimiter` |

### Behavioral Patterns
| Pattern | Where Used |
|---------|------------|
| **Strategy** | Payment processing strategies, notification channel strategies |
| **Observer/Event** | Kafka event publishing/consuming across all services |
| **State Machine** | Order status transitions (`CREATED → CONFIRMED → SHIPPED → DELIVERED`) |
| **Template Method** | Base entity classes, abstract notification handlers |
| **Chain of Responsibility** | Spring Security filter chain, Gateway filter chain |

### Architectural Patterns
| Pattern | Where Used |
|---------|------------|
| **SAGA (Orchestration)** | Distributed transactions: Order → Payment → Inventory |
| **CQRS** | Search service (read-optimized) separated from write services |
| **API Gateway** | Spring Cloud Gateway — single entry point |
| **Circuit Breaker** | Resilience4j on all inter-service HTTP calls |
| **Event Sourcing** | Kafka-based event log for order/payment state changes |
| **Database per Service** | Each microservice owns its database |

---

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.4.4 |
| **Cloud** | Spring Cloud | 2024.0.0 |
| **AI** | Spring AI (OpenAI) | 1.0.5 |
| **API Gateway** | Spring Cloud Gateway | — |
| **Security** | Spring Security + JWT (jjwt) | 0.12.6 |
| **Relational DB** | PostgreSQL | 16 |
| **NoSQL DB** | Apache Cassandra | 4.1 |
| **Cache** | Redis (Redisson) | 7 / 3.37.0 |
| **Messaging** | Apache Kafka | 7.7.0 (Confluent) |
| **Search Engine** | Elasticsearch | 8.15.0 |
| **Resilience** | Resilience4j | 2.2.0 |
| **Payment** | Stripe Java SDK | 28.2.0 |
| **API Docs** | SpringDoc OpenAPI | 2.7.0 |
| **Logging** | ELK Stack (Logstash + Kibana) | 8.15.0 |
| **Tracing** | Zipkin | latest |
| **Testing** | JUnit 5, Testcontainers, Gatling | — / 1.20.4 / 3.12.0 |
| **Build** | Maven (multi-module) | 3.9+ |
| **Containers** | Docker, Docker Compose | — |
| **Orchestration** | Kubernetes | — |

---

## Getting Started

### Prerequisites

| Tool | Version | Required |
|------|---------|----------|
| Java JDK | 21+ | Yes |
| Maven | 3.9+ | Yes |
| Docker & Docker Compose | Latest | Yes |
| Git | 2.x+ | Yes |

### Quick Start (Docker Compose)

**1. Clone the repository**

```bash
git clone https://github.com/your-org/ecommerce-platform.git
cd ecommerce-platform
```

**2. Start infrastructure services**

```bash
docker-compose up -d
```

This starts PostgreSQL, Cassandra, Redis, Kafka, Elasticsearch, Logstash, Kibana, and Zipkin.

**3. Build the project**

```bash
mvn clean install -DskipTests
```

**4. Run services (in order)**

```bash
# 1. Config Server (must start first)
cd config-server && mvn spring-boot:run &

# 2. Wait for Config Server, then start remaining services
cd api-gateway && mvn spring-boot:run &
cd auth-service && mvn spring-boot:run &
cd user-service && mvn spring-boot:run &
cd product-service && mvn spring-boot:run &
cd inventory-service && mvn spring-boot:run &
cd order-service && mvn spring-boot:run &
cd payment-service && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &
cd search-service && mvn spring-boot:run &
cd ai-recommendation-service && mvn spring-boot:run &
```

**5. Verify services are running**

```bash
# Check API Gateway health
curl http://localhost:8080/actuator/health

# Check all services via gateway
curl http://localhost:8080/actuator/gateway/routes
```

### Accessing the Platform

| Interface | URL |
|-----------|-----|
| API Gateway | http://localhost:8080 |
| Swagger UI (via gateway) | http://localhost:8080/swagger-ui.html |
| Kafka UI | http://localhost:8090 |
| Kibana | http://localhost:5601 |
| Zipkin | http://localhost:9411 |
| Elasticsearch | http://localhost:9200 |

---

## API Endpoints

All endpoints are accessible through the **API Gateway** at `http://localhost:8080`.

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT tokens |
| `POST` | `/api/auth/refresh` | Refresh an access token |

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users/{id}` | Get user profile |
| `PUT` | `/api/users/{id}` | Update user profile |
| `GET` | `/api/users/{id}/addresses` | Get user addresses |

### Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/products` | List products (paginated) |
| `GET` | `/api/products/{id}` | Get product details |
| `POST` | `/api/products` | Create a product (Admin) |
| `PUT` | `/api/products/{id}` | Update a product (Admin) |
| `GET` | `/api/products/categories` | List categories |

### Inventory

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/inventory/{productId}` | Check stock availability |
| `PUT` | `/api/inventory/{productId}` | Update stock (Admin) |

### Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/orders` | Place a new order |
| `GET` | `/api/orders/{id}` | Get order details |
| `GET` | `/api/orders/user/{userId}` | Get user's order history |
| `PUT` | `/api/orders/{id}/cancel` | Cancel an order |

### Payments

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/payments` | Process a payment |
| `GET` | `/api/payments/{id}` | Get payment status |
| `POST` | `/api/payments/{id}/refund` | Refund a payment |

### Search

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/search?q={query}` | Full-text product search |
| `GET` | `/api/search/suggest?q={prefix}` | Search auto-suggestions |

### AI Recommendations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/recommendations/{userId}` | Get personalized recommendations |
| `GET` | `/api/recommendations/similar/{productId}` | Get similar products |

### Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/notifications/{userId}` | Get user notifications |

---

## SAGA Order Flow

The platform uses the **SAGA Orchestration Pattern** for the order placement flow, ensuring data consistency across microservices without distributed transactions:

```
┌─────────┐     ┌──────────┐     ┌───────────┐     ┌──────────────┐
│  Client  │────▶│  Order   │────▶│  Payment  │────▶│  Inventory   │
│          │     │ Service  │     │  Service  │     │   Service    │
└─────────┘     └────┬─────┘     └─────┬─────┘     └──────┬───────┘
                     │                 │                   │
                     │  1. Create      │  2. Process       │  3. Reserve
                     │     Order       │     Payment       │     Stock
                     │                 │                   │
                     │◀────────────────┤◀──────────────────┤
                     │                 │                   │
                     │  On Failure: Compensating Transactions
                     │  ← Refund Payment ← Release Stock
                     │                 │                   │
                     ├─────────────────┴───────────────────┤
                     │                                     │
                     ▼                                     ▼
              ┌──────────────┐                   ┌──────────────┐
              │ Notification │                   │    Kafka     │
              │   Service    │◀──────────────────│  Event Bus   │
              └──────────────┘                   └──────────────┘
```

**Happy Path:**
1. **Order Service** creates order (`CREATED`) → publishes `OrderCreatedEvent`
2. **Payment Service** processes payment → publishes `PaymentCompletedEvent`
3. **Inventory Service** reserves stock → publishes `InventoryReservedEvent`
4. **Order Service** confirms order (`CONFIRMED`) → publishes `OrderConfirmedEvent`
5. **Notification Service** sends confirmation email

**Compensation (on failure):**
- Payment fails → Order marked `PAYMENT_FAILED`, stock released
- Inventory reservation fails → Payment refunded, Order marked `CANCELLED`

---

## Testing

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run tests for a specific service
mvn test -pl order-service

# Run with coverage report
mvn test jacoco:report
```

### Integration Tests

```bash
# Run integration tests (requires Docker for Testcontainers)
mvn verify -pl auth-service,order-service,payment-service,inventory-service -Pintegration-test
```

### Load Tests (Gatling)

```bash
# Run load tests
mvn gatling:test -pl load-tests
```

### Test Coverage

Coverage reports are generated at `{service}/target/site/jacoco/index.html` after running tests with JaCoCo.

---

## Deployment

### Docker

**Build all Docker images:**

```bash
# Build all services
mvn clean package -DskipTests

# Build Docker images
for service in config-server api-gateway auth-service user-service product-service \
    inventory-service order-service payment-service notification-service \
    search-service ai-recommendation-service; do
  docker build -t ecommerce/${service}:latest ${service}/
done
```

**Run with Docker Compose:**

```bash
docker-compose up -d
```

### Kubernetes

```bash
# Create namespace
kubectl create namespace ecommerce

# Apply configurations
kubectl apply -f k8s/ -n ecommerce

# Verify deployments
kubectl get pods -n ecommerce
kubectl get services -n ecommerce
```

### AWS (EKS)

```bash
# Create EKS cluster
eksctl create cluster --name ecommerce --region us-east-1 --nodes 3

# Deploy
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com

# Push images and apply K8s manifests
kubectl apply -f k8s/production/ -n ecommerce
```

---

## CI/CD

### Jenkins

The project includes a full `Jenkinsfile` with the following stages:

| Stage | Description |
|-------|-------------|
| Checkout | Clone repository |
| Build & Unit Tests | Compile and run unit tests with JaCoCo coverage |
| Integration Tests | Run integration tests with Testcontainers |
| SonarQube Analysis | Static code analysis and quality metrics |
| Quality Gate | Enforce SonarQube quality gate thresholds |
| Package | Build JAR artifacts for all services |
| Docker Build & Push | Build and push Docker images in parallel (11 services) |
| Deploy to Staging | Rolling update to staging K8s namespace |
| Smoke Tests | Health check verification on all endpoints |
| Deploy to Production | Manual approval gate, then rolling update to production |

### GitHub Actions

The `.github/workflows/ci.yml` pipeline provides:

- **Triggers**: Push to `main`/`develop`, pull requests to `main`
- **Build Job**: Compile, unit tests, coverage reports, package artifacts
- **Integration Tests**: Run with PostgreSQL service containers
- **Docker Build**: Matrix strategy for parallel image builds (11 services) with GHCR push
- **Deploy Staging**: Auto-deploy on `develop` branch merges
- **Deploy Production**: Auto-deploy on `main` branch merges (with environment protection rules)

---

## Monitoring & Observability

### ELK Stack (Centralized Logging)

| Component | URL | Purpose |
|-----------|-----|---------|
| **Kibana** | http://localhost:5601 | Log visualization & dashboards |
| **Elasticsearch** | http://localhost:9200 | Log storage & indexing |
| **Logstash** | `localhost:5000` | Log aggregation & pipeline |

All services ship structured JSON logs via Logstash Logback Encoder.

### Distributed Tracing

| Component | URL | Purpose |
|-----------|-----|---------|
| **Zipkin** | http://localhost:9411 | Request tracing across services |

### Kafka Monitoring

| Component | URL | Purpose |
|-----------|-----|---------|
| **Kafka UI** | http://localhost:8090 | Topic inspection, consumer groups, messages |

### Health Checks

Every service exposes Spring Boot Actuator endpoints:

```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Individual service health (direct)
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # User
curl http://localhost:8083/actuator/health  # Product
curl http://localhost:8084/actuator/health  # Inventory
curl http://localhost:8085/actuator/health  # Order
curl http://localhost:8086/actuator/health  # Payment
curl http://localhost:8087/actuator/health  # Notification
curl http://localhost:8088/actuator/health  # Search
curl http://localhost:8089/actuator/health  # AI Recommendation
```

---

## Project Structure

```
ecommerce-platform/
├── pom.xml                          # Parent POM (multi-module)
├── docker-compose.yml               # Infrastructure services
├── init-db.sql                      # Database initialization
├── Jenkinsfile                      # Jenkins CI/CD pipeline
├── .github/workflows/ci.yml         # GitHub Actions CI/CD
│
├── common-lib/                      # Shared library
│   └── src/main/java/com/ecommerce/common/
│       ├── dto/                     # Shared DTOs & API responses
│       ├── event/                   # Kafka event definitions
│       └── exception/               # Global exception handling
│
├── config-server/                   # Centralized configuration
│   └── src/main/resources/
│       └── configurations/          # Service-specific configs
│
├── api-gateway/                     # API Gateway (Spring Cloud)
│   └── src/main/java/.../gateway/
│       ├── config/                  # Route & security config
│       └── filter/                  # JWT validation filters
│
├── auth-service/                    # Authentication & Authorization
│   └── src/main/java/.../auth/
│       ├── controller/              # REST endpoints
│       ├── service/                 # Business logic
│       ├── model/                   # JPA entities
│       ├── repository/              # Data access
│       └── security/                # JWT & Spring Security
│
├── user-service/                    # User Management
├── product-service/                 # Product Catalog
├── inventory-service/               # Stock Management (Cassandra)
├── order-service/                   # Order Processing & SAGA
├── payment-service/                 # Payment Processing (Stripe)
├── notification-service/            # Email Notifications
├── search-service/                  # Elasticsearch Search
├── ai-recommendation-service/       # AI Recommendations (Spring AI)
│
├── elk/                             # ELK stack configuration
│   └── logstash/
│       └── logstash.conf            # Logstash pipeline config
│
└── load-tests/                      # Gatling performance tests
```

---

## Contributing

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/my-feature`
3. **Commit** your changes: `git commit -m 'Add my feature'`
4. **Push** to the branch: `git push origin feature/my-feature`
5. **Open** a Pull Request

### Branch Naming Convention

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New features | `feature/wishlist-service` |
| `bugfix/` | Bug fixes | `bugfix/order-status-update` |
| `hotfix/` | Production fixes | `hotfix/payment-timeout` |
| `refactor/` | Code refactoring | `refactor/saga-pattern` |

### Commit Message Convention

```
<type>(<scope>): <description>

feat(order-service): add order cancellation endpoint
fix(payment-service): handle Stripe webhook timeout
docs(readme): update API endpoints table
test(auth-service): add JWT refresh token tests
```

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Built with Spring Boot, Kafka, Redis, and a passion for microservices architecture.
</p>
