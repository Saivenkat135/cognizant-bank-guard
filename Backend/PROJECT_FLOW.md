# BankGuard Backend — Complete Project Flow Guide

> A beginner-friendly walkthrough of every microservice in this `Backend/` folder,
> starting from Spring Boot fundamentals → how the API Gateway works →
> how a single transaction travels through the whole system.

---

## Table of Contents

1. [Spring Boot Crash Course (read this first)](#1-spring-boot-crash-course-read-this-first)
2. [Big Picture: How the Microservices Talk](#2-big-picture-how-the-microservices-talk)
3. [Service Map — Ports & Responsibilities](#3-service-map--ports--responsibilities)
4. [Boot Order: Which Service to Start First](#4-boot-order-which-service-to-start-first)
5. [Deep Dive: apiGateway](#5-deep-dive-apigateway)
6. [Deep Dive: EurekaServer (Service Registry)](#6-deep-dive-eurekaserver-service-registry)
7. [Deep Dive: ConfigServer (Central Configuration)](#7-deep-dive-configserver-central-configuration)
8. [Deep Dive: transactionService](#8-deep-dive-transactionservice)
9. [Deep Dive: enrichmentService](#9-deep-dive-enrichmentservice)
10. [Deep Dive: Decision_Engine_service (Gemini AI)](#10-deep-dive-decision_engine_service-gemini-ai)
11. [Deep Dive: AlertCaseService](#11-deep-dive-alertcaseservice)
12. [Deep Dive: sarReport](#12-deep-dive-sarreport)
13. [End-to-End: Tracing a Single Transaction](#13-end-to-end-tracing-a-single-transaction)

---

## 1. Spring Boot Crash Course (read this first)

Before any of the service flows make sense, you need a mental model of the
**3 layers** every Spring Boot service in this project follows:

```
   HTTP Request
       │
       ▼
┌──────────────────┐
│   CONTROLLER     │  ←  @RestController / @RequestMapping
│  (the "door")    │     Decides "which method should answer this URL?"
└──────────────────┘     Reads JSON body, returns JSON response.
       │
       ▼
┌──────────────────┐
│    SERVICE       │  ←  @Service
│  (the "brain")   │     Holds the business logic.
└──────────────────┘     Calls other services or repositories.
       │
       ▼
┌──────────────────┐
│   REPOSITORY     │  ←  @Repository  (extends JpaRepository / ReactiveCrudRepository)
│  (the "hands")   │     Talks to the database (MySQL).
└──────────────────┘     You write zero SQL — Spring generates it.
       │
       ▼
   MySQL / Database
```

### Other Spring annotations you'll see everywhere

| Annotation | What it means in plain English |
|---|---|
| `@SpringBootApplication` | "This class is the entry point. Boot the whole app from here." Put on the `*Application.java` file with `main()`. |
| `@RestController` | "This class handles HTTP requests and returns JSON." |
| `@RequestMapping("/api/x")` | "Every URL in this class starts with `/api/x`." |
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | The HTTP verb for one method. GET = read, POST = create, PUT = update, DELETE = remove. |
| `@PathVariable` | Pull a value out of the URL: `/users/{id}` → `id` parameter. |
| `@RequestBody` | "Take the JSON the client sent and turn it into a Java object." |
| `@Service` | "This class holds business logic. Spring, please create one instance and inject it where needed." |
| `@Repository` | "This interface talks to the database." |
| `@Entity` + `@Table` | "This class maps to a database table." |
| `@Autowired` / constructor injection | "Spring, please give me the bean I need" — that's how the controller gets the service, and the service gets the repository. |
| `@Configuration` + `@Bean` | "This class defines beans (objects) Spring should manage for me — e.g. a `WebClient`, a `PasswordEncoder`." |
| `@Value("${some.property}")` | Pull a value from `application.properties` / `application.yml`. |

### Other building blocks used in this project

- **DTO (Data Transfer Object)** — a plain Java class used to *carry data between services* or between client and server. Lives in `dto/`. It is NOT a database row. It's just a JSON shape.
- **Entity** — a Java class that *maps to a database table*. Lives in `entity/`.
- **WebClient** — Spring's modern HTTP client. When one microservice calls another, it uses `WebClient` to send an HTTP request.
- **Reactive (WebFlux) vs MVC** — Most services here are normal MVC (synchronous, blocking). The **apiGateway** is **reactive** (uses `Mono` and `Flux`), because Spring Cloud Gateway is built on WebFlux. Reactive code looks weird at first (`Mono.just(...)`, `.flatMap(...)`), but it's still the same controller-service-repository pattern.

> **`Mono<T>`** = "a promise of *zero or one* T in the future."
> **`Flux<T>`** = "a promise of *zero or more* Ts in the future."

---

## 2. Big Picture: How the Microservices Talk

This is a **fraud-detection system** for banking transactions. When a customer
sends money, the request travels through ~5 services before the bank decides
whether to allow, flag, or block it.

```
                           ┌──────────────────────────┐
                           │   Frontend (React)       │
                           │   localhost:5173         │
                           └────────────┬─────────────┘
                                        │ HTTP + JWT
                                        ▼
                           ┌──────────────────────────┐
                           │  apiGateway  (port 1001) │  ← single entry door
                           │  - login / register      │  ← issues JWT
                           │  - role-based routing    │  ← checks JWT
                           │  - forwards to services  │
                           └────────────┬─────────────┘
                                        │
                ┌───────────────────────┼───────────────────────────┐
                │                       │                           │
                ▼                       ▼                           ▼
   ┌────────────────────┐   ┌────────────────────┐   ┌────────────────────────┐
   │ transactionService │   │ enrichmentService  │   │ Decision_Engine        │
   │   (port 8089)      │──▶│   (port 8010)      │──▶│   (port 7002, Gemini)  │
   │  POST /api/        │   │  POST /api/enrich  │   │  POST /api/gemini      │
   │  transactions      │   │  /transaction/...  │   │  /analyze-transaction  │
   └────────────────────┘   └─────────┬──────────┘   └────────────────────────┘
                                      │ (only when flagged/terminated)
                                      ▼
                            ┌────────────────────┐
                            │ AlertCaseService   │
                            │   (port 8085)      │
                            │  - creates case    │
                            │  - emails customer │
                            └─────────┬──────────┘
                                      │
                                      ▼
                            ┌────────────────────┐
                            │   sarReport        │
                            │   (port 8088)      │
                            │  - saves SAR for   │
                            │    regulators      │
                            └────────────────────┘

   Supporting infrastructure (always running in the background):
   ┌────────────────────┐         ┌────────────────────┐
   │  ConfigServer      │         │   EurekaServer     │
   │  (port 8888)       │         │   (port 8761)      │
   │  Central config    │         │ Service registry — │
   │  pulled from Git   │         │ "who is alive,     │
   │                    │         │  who is at what IP"│
   └────────────────────┘         └────────────────────┘
```

**Two key infrastructure ideas:**

1. **Eureka = service phone book.** Every microservice "checks in" with Eureka on startup. When service A wants to call service B, it asks Eureka "where is B?" and gets back an IP+port. This way services don't hardcode each other's URLs.

2. **ConfigServer = shared config.** Instead of every service having its own `application.properties` with DB URLs, Eureka URLs, secrets, etc., the ConfigServer pulls them from a Git repo (`https://github.com/raj-singh-23/config-server`) and hands them out to each service when it boots.

---

## 3. Service Map — Ports & Responsibilities

| Service | Port | Role | Database |
|---|---|---|---|
| **ConfigServer** | 8888 | Central configuration (pulls from Git) | none |
| **EurekaServer** | 8761 | Service registry / discovery | none |
| **apiGateway** | 1001 | Single entry door, auth, JWT, routing | `bankguard_auth` (R2DBC, reactive MySQL) |
| **transactionService** | 8089 | Customer accounts + transactions | (configured via ConfigServer) |
| **enrichmentService** | 8010 | Adds context, calls Gemini, routes alerts | none (stateless) |
| **Decision_Engine_service** | 7002 | Calls Gemini AI, stores risk rules | `decision_db` |
| **AlertCaseService** | 8085 | Creates fraud cases, sends customer email | `alert_case_db` (via ConfigServer) |
| **sarReport** | 8088 | Suspicious Activity Reports (regulatory) | `report` |

---

## 4. Boot Order: Which Service to Start First

Because services depend on each other, the **order matters**:

1. **ConfigServer** (8888) — others fail to start if config is missing.
2. **EurekaServer** (8761) — others need to register.
3. **apiGateway** (1001) — needs ConfigServer to load route definitions.
4. **All business services** in any order: transactionService, enrichmentService, Decision_Engine_service, AlertCaseService, sarReport.

If a service can't reach ConfigServer it will fail fast — that's intentional
(see `spring.cloud.config.fail-fast=true` in every service's properties).

---

## 5. Deep Dive: apiGateway

**Folder:** [apiGateway/](apiGateway/)
**Port:** 1001
**Type:** Spring Cloud Gateway (reactive / WebFlux)
**Why it's special:** This is the *only* service the frontend talks to directly. It is the security boundary.

### 5.1 What the apiGateway does (4 jobs)

1. **Authentication** — issues JWT tokens at `/auth/login`, accepts new signups at `/auth/register`.
2. **Authorization** — on every request, reads the `Authorization: Bearer <token>` header, validates the JWT, and decides whether the caller's role (SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER, CUSTOMER) is allowed to hit that URL.
3. **Routing** — forwards approved requests to the correct downstream microservice. The route definitions live in the external **ConfigServer** repo (not in this folder).
4. **CORS** — allows the React frontend running on `localhost:5173`–`5177` to call it.

### 5.2 File-by-file tour

#### Entry point — [ApiGatewayApplication.java](apiGateway/src/main/java/com/cts/apiGateway/ApiGatewayApplication.java)
Just `main()` + `@SpringBootApplication`. Nothing fancy. Boot.

#### Controllers (the "doors")

**[AuthController.java](apiGateway/src/main/java/com/cts/apiGateway/controller/AuthController.java)** — `@RequestMapping("/auth")`

| Method | URL | What it does |
|---|---|---|
| `POST /auth/register` | Creates a new user with `isApproved = false`. SUPER_ADMIN signup is forbidden (those are seeded in DB). |
| `POST /auth/login` | Verifies password, blocks unapproved users, returns a **JWT token**. |

Flow inside `register`:
1. Parse role string → `Role` enum. If invalid → 400.
2. If role is `SUPER_ADMIN` → 403 (you can't register as superadmin).
3. Check if username exists → 409 conflict.
4. Otherwise: hash password with BCrypt → save user (`isApproved=false`) → return 201.

Flow inside `login`:
1. Find user by username. If not found → 401.
2. Check BCrypt-hashed password matches. If not → 401.
3. If `isApproved=false` and role isn't SUPER_ADMIN → 403 ("Pending approval").
4. Otherwise: build a JWT with username+role → return it.

> The whole class is reactive — every method returns `Mono<ResponseEntity<...>>` instead of a plain `ResponseEntity<...>`. That's because Spring Cloud Gateway runs on WebFlux, not MVC.

**[AdminUserController.java](apiGateway/src/main/java/com/cts/apiGateway/controller/AdminUserController.java)** — `@RequestMapping("/auth/users")`
SUPER_ADMIN-only endpoints to approve pending signups (FraudAnalyst, RiskManager):

| Method | URL | What |
|---|---|---|
| `GET /auth/users` | List all users |
| `GET /auth/users/pending` | List unapproved users |
| `PUT /auth/users/{id}/approve` | Flip `isApproved` → true |
| `DELETE /auth/users/{id}` | Reject (delete) the signup |

#### Models (DB rows + role enum)

**[User.java](apiGateway/src/main/java/com/cts/apiGateway/model/User.java)** — maps to the `users` table. Fields: `id, username, password (BCrypt-hashed), role, isApproved`.

**[Role.java](apiGateway/src/main/java/com/cts/apiGateway/model/Role.java)** — the four roles:
```java
SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER, CUSTOMER
```

#### Repository (DB access)

**[UserRepository.java](apiGateway/src/main/java/com/cts/apiGateway/repository/UserRepository.java)** — extends `ReactiveCrudRepository<User, Long>`.

You wrote NO SQL. Spring sees method names like `findByUsername`, `findByRole`, `findByIsApproved` and **auto-generates the SQL** at runtime. That's the magic of Spring Data.

Reactive flavor: returns `Mono<User>` (zero/one) or `Flux<User>` (many).

#### Security layer — the heart of the gateway

**[JwtUtil.java](apiGateway/src/main/java/com/cts/apiGateway/security/JwtUtil.java)** — helper for building / parsing JWT tokens.
- `generateToken(username, role)` — issues a token signed with HS256.
- `extractUsername(token)`, `extractRole(token)` — pulls claims back out.
- `isTokenValid(token)` — checks signature + expiry.
- Secret + expiry come from `application.properties`:
  - `jwt.secret=BankGuardSuperSecretKeyForJWT2025...`
  - `jwt.expiration=86400000` (24 h in ms)

**[JwtAuthenticationFilter.java](apiGateway/src/main/java/com/cts/apiGateway/security/JwtAuthenticationFilter.java)** — runs on **every** incoming request:
1. If the path is `/auth/login` or `/auth/register` → skip JWT check (public).
2. Otherwise look for `Authorization: Bearer <token>` header.
3. If missing → 401.
4. If present → validate it. Extract username + role.
5. Stuff a Spring Security `Authentication` object with `ROLE_<role>` into the reactive security context. Now SecurityConfig's rules know who you are.

**[SecurityConfig.java](apiGateway/src/main/java/com/cts/apiGateway/config/SecurityConfig.java)** — the rulebook of who can hit which URL. This is the most important file to understand for routing+auth.

Key rules (read from top to bottom — first match wins):

| Path | Method | Allowed roles |
|---|---|---|
| `/auth/login`, `/auth/register` | POST | **public** |
| `/auth/users/**` | any | SUPER_ADMIN only |
| `/api/transactions/**` | GET | SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER, CUSTOMER |
| `/api/transactions/**` | POST/PUT/DELETE | SUPER_ADMIN |
| `/api/customers/**` | GET | all four roles |
| `/api/customers/**` | POST/PUT/DELETE | SUPER_ADMIN |
| `/api/investigation/**` | GET | SUPER_ADMIN, FRAUD_ANALYST |
| `/api/investigation/cases/*/status` | PUT | SUPER_ADMIN, FRAUD_ANALYST |
| `/sar/**` | GET | SUPER_ADMIN, FRAUD_ANALYST |
| `/sar/**` | POST | SUPER_ADMIN |
| `/api/enrich/**` | any | SUPER_ADMIN, RISK_MANAGER |
| `/api/gemini/**` | any | SUPER_ADMIN, RISK_MANAGER |
| anything else | any | must be authenticated |

It also configures:
- **BCrypt** as the password encoder (`@Bean PasswordEncoder`)
- **CORS** for the five Vite dev ports
- A plain 401 response on auth failure (no `WWW-Authenticate: Basic` header, so browsers don't show the native login popup)

**[DatabaseConfig.java](apiGateway/src/main/java/com/cts/apiGateway/config/DatabaseConfig.java)** — runs [schema.sql](apiGateway/src/main/resources/schema.sql) on startup, creating the `users` table and seeding the `is_approved` column. `continueOnError=true` because the ALTER statement will fail on a fresh DB where the column already exists from CREATE TABLE.

#### Error handler

**[GlobalExceptionHandler.java](apiGateway/src/main/java/com/cts/apiGateway/handler/GlobalExceptionHandler.java)** — catches anything that blows up at the gateway layer (JWT errors, routing errors, etc.) and returns a clean JSON error body instead of a stack trace. `@Order(-2)` runs it before Spring's default error handler.

#### DTOs

- `AuthRequest` — `{ username, password }` (login body)
- `RegisterRequest` — `{ username, password, role }` (signup body)
- `AuthResponse` — `{ token, username, role, message }`
- `UserView` — admin's view of a user row (no password leaked)

### 5.3 Config — [application.properties](apiGateway/src/main/resources/application.properties)

```properties
spring.application.name=apiGateway
server.port=1001
spring.main.web-application-type=reactive          # MUST be reactive for Cloud Gateway
spring.config.import=configserver:http://localhost:8888
spring.cloud.config.fail-fast=true                 # don't start if config server is down
spring.r2dbc.url=r2dbc:mysql://localhost:3306/bankguard_auth   # reactive MySQL driver
jwt.secret=...
jwt.expiration=86400000
```

> **`r2dbc:` vs `jdbc:`** — R2DBC is the reactive MySQL driver. It returns `Mono`/`Flux` from queries instead of blocking the thread. All the other services use plain JDBC because they're not reactive.

### 5.4 How a request flows through the gateway

Imagine the frontend sends `GET /api/transactions/customer/42` with `Authorization: Bearer eyJ...`:

1. CORS preflight passes (origin is allowed).
2. `GlobalExceptionHandler` is on standby in case something throws.
3. `JwtAuthenticationFilter.filter()` runs:
   - Path is not public → look for token.
   - Token is valid → extract role (e.g. `FRAUD_ANALYST`) → push `Authentication` into security context.
4. `SecurityConfig` rule for `GET /api/transactions/**` allows `FRAUD_ANALYST` → request proceeds.
5. Spring Cloud Gateway looks up the route definition (loaded from ConfigServer's Git repo) and **forwards** the request to `transactionService` on port 8089.
6. transactionService returns the response → gateway streams it back to the browser.

> **Where are the route forwarding rules?** They are not in this repo — they live in the **ConfigServer's Git repo** (`https://github.com/raj-singh-23/config-server`) and are fetched at startup. That's why `apiGateway` here has no `RouteLocator` bean — Spring builds routes from properties supplied by the config server.

---

## 6. Deep Dive: EurekaServer (Service Registry)

**Folder:** [EurekaServer/](EurekaServer/)
**Port:** 8761
**Files:** [EurekaServerApplication.java](EurekaServer/src/main/java/com/cts/eurekaserver/EurekaServerApplication.java), [application.yml](EurekaServer/src/main/resources/application.yml)

### What it does
Eureka is a **service registry** — a phone book for microservices.

When a service like `enrichmentService` starts, it sends a heartbeat to Eureka saying "I'm enrichmentService, I'm at IP X, port 8010, I'm alive." Every 30 seconds it pings again. If it stops pinging, Eureka evicts it.

When `enrichmentService` wants to call `alert-case-service`, it doesn't hardcode `http://localhost:8085`. Instead it asks the load-balanced `WebClient` to call `http://alert-case-service` and Spring asks Eureka "what's the real address?" — then routes there. See [AlertCaseClient.java](enrichmentService/src/main/java/com/bankguard/enrichmentservice/client/AlertCaseClient.java) for an example: `baseUrl("http://alert-case-service")` — that's a logical name resolved via Eureka, not a real hostname.

### The code
```java
@SpringBootApplication
@EnableEurekaServer   // ← one annotation turns this into a Eureka server
public class EurekaServerApplication { ... }
```

### Config — [application.yml](EurekaServer/src/main/resources/application.yml)
```yaml
server.port: 8761
spring.application.name: eureka-server
eureka.client.register-with-eureka: false   # The server itself doesn't register
eureka.client.fetch-registry: false
eureka.server.enable-self-preservation: false   # dev mode: evict dead services quickly
```

Open `http://localhost:8761` in a browser after starting it — you'll see a dashboard with every registered service.

---

## 7. Deep Dive: ConfigServer (Central Configuration)

**Folder:** [ConfigServer/](ConfigServer/)
**Port:** 8888
**Files:** [ConfigServerApplication.java](ConfigServer/src/main/java/com/cts/ConfigServer/ConfigServerApplication.java), [application.yaml](ConfigServer/src/main/resources/application.yaml)

### What it does
Instead of every service carrying its own DB URLs, Eureka URLs, and gateway route rules in its `application.properties`, the config server fetches all of that from a **Git repository** and hands it out on demand.

Every service has this line in its `application.properties` / `application.yml`:
```properties
spring.config.import=configserver:http://localhost:8888
```
On startup, the service contacts `localhost:8888`, asks "give me the config for `transactionService` profile `dev`," and the config server replies with the merged YAML pulled from Git.

### The code
```java
@SpringBootApplication
@EnableConfigServer   // ← turns this app into a config server
public class ConfigServerApplication { ... }
```

### Config — [application.yaml](ConfigServer/src/main/resources/application.yaml)
```yaml
server.port: 8888
spring.cloud.config.server.git.uri: https://github.com/raj-singh-23/config-server
spring.cloud.config.server.git.default-label: main
spring.cloud.config.server.git.clone-on-start: true
```

> **Important:** This is where the API Gateway routes live. To add a new
> microservice and have the gateway forward requests to it, you edit the
> Git repo above (not this folder).

---

## 8. Deep Dive: transactionService

**Folder:** [transactionService/](transactionService/)
**Port:** 8089
**Purpose:** Manages **customers** (account info, login, balance) and **transactions** (money movement between accounts).

This is a "classic" Spring Boot MVC service — no reactive code. It uses JPA + Hibernate to talk to MySQL.

### 8.1 Structure
```
transactionService/
└── src/main/java/.../transactionservice/
    ├── TransactionServiceApplication.java     ← entry point, @SpringBootApplication
    ├── controller/
    │   ├── CustomerController.java            ← /api/customers/**
    │   └── TransactionController.java         ← /api/transactions/**
    ├── service/
    │   ├── CustomerService.java
    │   ├── TransactionService.java
    │   └── TransactionEnrichmentIntegrationService.java   ← calls enrichmentService
    ├── entity/
    │   ├── Customer.java                      ← customers table
    │   └── Transaction.java                   ← transactions table
    ├── repository/
    │   ├── CustomerRepository.java            ← Spring Data JPA
    │   └── TransactionRepository.java
    ├── dto/                                   ← request/response shapes
    └── config/
        ├── CorsConfig.java
        └── WebTemplateConfig.java             ← defines the WebClient bean
```

### 8.2 Controllers

**[CustomerController.java](transactionService/src/main/java/com/bankguard/transactionservice/controller/CustomerController.java)** — `/api/customers`

Standard CRUD: create, read, update, delete customers, plus `POST /api/customers/login` for customer login and lookup by email or account number.

**[TransactionController.java](transactionService/src/main/java/com/bankguard/transactionservice/controller/TransactionController.java)** — `/api/transactions`

The most important endpoint is `POST /api/transactions` which **creates a new transaction**. Walking through it:

**Step 1 — Validate** (before touching the DB):
1. Sender customer exists?
2. Amount > 0?
3. Sender has enough balance? → throws `TransactionProcessingException("INSUFFICIENT_BALANCE")`.
4. Receiver account number exists? → throws `ReceiverAccountNotFoundException`.

**Step 2 — Build the Transaction object** (city, state, IP, time, initial risk score).

**Step 3 — Call enrichmentService** via `TransactionEnrichmentIntegrationService.enrichTransactionWithService(...)`. This is a **blocking HTTP call** to `http://localhost:8010/api/enrich/transaction/with-decision-and-alert`. The response carries:
- A **decision**: `genuine`, `flagged`, or `terminated`
- A **risk score** (0–100)
- A **reason** (Gemini's explanation)

**Step 4 — Recalculate the customer's overall risk score** as an average of their previous transactions' risk scores + the new one. Clamp to [0, 100].

**Step 5 — Decide whether to move money:**
- If decision is `terminated` → DO NOT move money. Still save the transaction with status `terminated` and update the sender's risk score.
- Otherwise (`genuine` or `flagged`) → debit sender, credit receiver.

**Step 6 — Save everything to MySQL** (sender row, receiver row, transaction row).

**Step 7 — Return** a JSON response with the transaction ID, new balances, decision, risk score.

The rest of the controller is standard CRUD: get all, get by ID, update, delete, get by customer ID, get by receiver account.

### 8.3 The integration service — how transactionService calls enrichmentService

[TransactionEnrichmentIntegrationService.java](transactionService/src/main/java/com/bankguard/transactionservice/service/TransactionEnrichmentIntegrationService.java)

Key idea: it bundles **(current transaction + customer profile + last 5 previous transactions)** into a single `EnrichmentRequestDTO` and POSTs it to enrichmentService:

```java
// Pseudocode:
EnrichmentRequestDTO req = new EnrichmentRequestDTO();
req.setCurrentTransaction(currentTxDTO);
req.setCustomer(customerDTO);
req.setPreviousTransactions(last5Transactions);

TransactionDecisionResponse resp = webClient.post()
        .uri("http://localhost:8010/api/enrich/transaction/with-decision-and-alert")
        .bodyValue(req)
        .retrieve()
        .bodyToMono(TransactionDecisionResponse.class)
        .block();   // .block() = wait for response (synchronous)
```

### 8.4 Entities

- **[Customer.java](transactionService/src/main/java/com/bankguard/transactionservice/entity/Customer.java)** — `customers` table. Fields: customerId, name, email, accountNo, balance, accountType, bankName, riskScore, password (for customer login), transactions (one-to-many).
- **[Transaction.java](transactionService/src/main/java/com/bankguard/transactionservice/entity/Transaction.java)** — `transactions` table. Fields: transactionId, amount, city, state, ipAddress, time, riskScore, receiverAccountNumber, status (`pending`/`genuine`/`flagged`/`terminated`), customerId, reason.

### 8.5 Config — [application.properties](transactionService/src/main/resources/application.properties)
```properties
spring.application.name=transactionService
server.port=8089
spring.config.import=configserver:http://localhost:8888
spring.cloud.config.profile=dev
spring.cloud.config.fail-fast=true
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
```

The DB URL, JPA settings, and `enrichment.service.url` come from the ConfigServer's Git repo, NOT from this file.

---

## 9. Deep Dive: enrichmentService

**Folder:** [enrichmentService/](enrichmentService/)
**Port:** 8010
**Purpose:** Take a raw transaction, **enrich it** with customer info + previous transactions, **call Gemini** for an AI decision, and **route alerts** to AlertCaseService when fraud is detected.

This is the orchestrator between transactionService, Decision_Engine_service, and AlertCaseService.

### 9.1 Controller — [EnrichmentController.java](enrichmentService/src/main/java/com/bankguard/enrichmentservice/controller/EnrichmentController.java)

`@RequestMapping("/api/enrich")`

Main endpoint:
- `POST /api/enrich/transaction/with-decision-and-alert` — accepts `EnrichmentRequest`, returns `TransactionDecisionResponse`.

Plus three simple validation helpers:
- `POST /api/enrich/validate/amount` — `amount > 0`?
- `POST /api/enrich/validate/balance` — `balance >= amount`?
- `POST /api/enrich/validate/ip` — regex check.

### 9.2 Service — [EnrichmentService.java](enrichmentService/src/main/java/com/bankguard/enrichmentservice/service/EnrichmentService.java)

The orchestrator method is `enrichAndDecideWithConditionalAlert(EnrichmentRequest)`:

**Step 1 — Enrich the transaction.** Build an `EnrichedTransactionDTO` from:
- the current transaction's amount, city, state, time
- the customer's name, email, accountNo, balance
- the last 5 previous transactions

**Step 2 — Convert to `DecisionRequest`.** Combine "city + state" into one `location` field. Pack customer info and previous transactions into the shape the Decision Engine expects.

**Step 3 — Call Decision_Engine_service (Gemini).**
```java
String url = decisionEngineBaseUrl + "/api/gemini/analyze-transaction";
GeminiDecisionResponse geminiDecision = webClient.post()...block();
```
Returns `{ riskScore, decision, reason }`. The default decision-engine URL is `http://localhost:7002` (overridable in ConfigServer).

If the call fails or returns 400, the service builds a fake `flagged` response so the system stays safe-by-default.

**Step 4 — Build the response.** Copy enriched fields + Gemini's decision/risk/reason into a `TransactionDecisionResponse`. Default `alertSent = false`.

**Step 5 — Conditional alert routing.** If the decision is **`flagged`** or **`terminated`** (after trim+lowercase):
1. Build an `AlertCasePayload` with all the customer info, amount, decision, risk score, reason.
2. Call `AlertCaseClient.sendToAlertCase(payload)` → POST to AlertCaseService.
3. Mark `response.alertSent = true`.

If the decision is `genuine` → no alert.

### 9.3 AlertCaseClient — service-to-service call

[AlertCaseClient.java](enrichmentService/src/main/java/com/bankguard/enrichmentservice/client/AlertCaseClient.java)

```java
@Component
public class AlertCaseClient {
    public AlertCaseClient(@Qualifier("loadBalancedWebClient") WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://alert-case-service").build();
    }

    public void sendToAlertCase(AlertCasePayload payload) {
        webClient.post()
                .uri("/api/investigation/ingest-fraud-alert")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
```

The base URL `http://alert-case-service` is **NOT a real DNS name** — it is the Eureka-registered name of AlertCaseService. The `@LoadBalanced` WebClient builder (defined in [WebTemplateConfig.java](enrichmentService/src/main/java/com/bankguard/enrichmentservice/config/WebTemplateConfig.java)) asks Eureka to resolve it to a real IP+port at call time.

### 9.4 Entry point — [EnrichmentServiceApplication.java](enrichmentService/src/main/java/com/bankguard/enrichmentservice/EnrichmentServiceApplication.java)
Plain `@SpringBootApplication`. The bean magic is in `WebTemplateConfig`.

---

## 10. Deep Dive: Decision_Engine_service (Gemini AI)

**Folder:** [Decision_Engine_service/](Decision_Engine_service/)
**Port:** 7002
**Purpose:** Take a `DecisionRequest`, run it through **Google's Gemini AI**, and return a fraud risk decision. Also stores **Risk Manager rules** in a MySQL table so the Risk Manager can tune detection without redeploying.

### 10.1 Controllers

**[GeminiController.java](Decision_Engine_service/src/main/java/com/cts/gemini_test_try2/Controller/GeminiController.java)** — `/api/gemini`

- `POST /api/gemini/analyze-transaction` — Accepts a `DecisionRequest`, returns a `GeminiDecisionResponse` ({ riskScore, decision, reason }). Called by enrichmentService.

**[DecisionRuleController.java](Decision_Engine_service/src/main/java/com/cts/gemini_test_try2/Controller/DecisionRuleController.java)** — `/api/gemini/rules`

CRUD for Risk Manager rules (mounted under `/api/gemini/rules` so the gateway's existing `/api/gemini/**` route forwards them here — no new gateway route needed):

| Method | URL | What |
|---|---|---|
| `GET /api/gemini/rules` | list all rules |
| `POST /api/gemini/rules` | add `{ text, riskScore }` |
| `DELETE /api/gemini/rules/{id}` | remove a rule |

### 10.2 Service — [GeminiService.java](Decision_Engine_service/src/main/java/com/cts/gemini_test_try2/Service/GeminiService.java)

The brain. `analyzeTransactionWithGemini(DecisionRequest)`:

1. **Extract previous location** — pulls the state name from the most recent previous transaction.
2. **Calculate average amount** of previous transactions.
3. **Apply two heuristic validations** that nudge the risk score before asking Gemini:
   - If `current amount <= average + 5%` → risk +10 (unusually small / fits pattern → suspicious in this rules-of-thumb model).
   - If `current state == previous state` → risk +20.
4. **Load Risk Manager rules** from `decision_rules` table via `DecisionRuleRepository`.
5. **Build a giant text prompt** for Gemini that includes: the Risk Manager rules, the transaction details, customer profile, validation results, and instructions to respond with strict JSON: `{ riskScore, decision, reason }`.
6. **Call Gemini** with `client.models.generateContent("gemini-3-flash-preview", prompt, null)`.
7. **Parse the JSON** out of Gemini's reply. Fall back to a `flagged` decision if parsing fails.

### 10.3 Entity + Repository

**[DecisionRule.java](Decision_Engine_service/src/main/java/com/cts/gemini_test_try2/entity/DecisionRule.java)** — `decision_rules` table: `{ id, ruleText (up to 1000 chars), riskScore }`.

**[DecisionRuleRepository.java](Decision_Engine_service/src/main/java/com/cts/gemini_test_try2/repository/DecisionRuleRepository.java)** — `extends JpaRepository<DecisionRule, Long>`. Free `findAll()`, `save()`, `deleteById()` etc.

### 10.4 Gemini bean — [GeminiConfig.java](Decision_Engine_service/src/main/java/com/cts/gemini_test_try2/config/GeminiConfig.java)
```java
@Configuration
public class GeminiConfig {
    @Value("${google.api.key}")
    private String apiKey;

    @Bean
    public Client geminiClient() {
        return Client.builder().apiKey(apiKey).build();
    }
}
```
That `Client` is auto-injected into `GeminiService` via `@Autowired`.

### 10.5 Config — [application.properties](Decision_Engine_service/src/main/resources/application.properties)
```properties
spring.application.name=decision-engine-service
server.port=7002
google.api.key=AQ.Ab8...                # Google Gemini key
spring.datasource.url=jdbc:mysql://localhost:3306/decision_db
spring.jpa.hibernate.ddl-auto=update    # Hibernate creates/updates schema
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

---

## 11. Deep Dive: AlertCaseService

**Folder:** [AlertCaseService/](AlertCaseService/)
**Port:** 8085
**Purpose:** When enrichmentService detects fraud (decision = `flagged` or `terminated`), this service:
1. Creates an **Alert** record (severity HIGH/MEDIUM/LOW based on risk score).
2. Creates a **Case** record (status = `OPEN`).
3. Sends an **email** to the customer via the Brevo API.
4. Forwards a `ReportingRequest` to sarReport so the Suspicious Activity Report gets filed.

This service also exposes APIs the Fraud Analyst frontend uses to view/update cases.

### 11.1 Controllers

**[AlertCaseController.java](AlertCaseService/src/main/java/com/cts/AlertCaseService/controller/AlertCaseController.java)** — `/api/investigation`

| Method | URL | Who | What |
|---|---|---|---|
| `POST /api/investigation/ingest-fraud-alert` | (internal) | called by enrichmentService when fraud is detected |
| `GET /api/investigation/alerts` | analyst | list all alerts |
| `GET /api/investigation/alerts/{alertId}` | analyst | single alert |
| `GET /api/investigation/alerts/severity/{severity}` | analyst | filter by severity |
| `GET /api/investigation/cases/{caseId}` | analyst | get one case |
| `GET /api/investigation/cases/status/{status}` | analyst | filter cases by status (OPEN/CLOSED/etc.) |
| `PUT /api/investigation/cases/{caseId}/status?status=CLOSED` | analyst | update case status |
| `GET /api/investigation/customers/{customerId}` | analyst | customer + their cases |
| `GET /api/investigation/customers/{customerId}/cases` | analyst | cases-only |

**[EmailController.java](AlertCaseService/src/main/java/com/cts/AlertCaseService/controller/EmailController.java)** — exposes the EmailService for manual testing.

### 11.2 Service — [FraudInvestigationService.java](AlertCaseService/src/main/java/com/cts/AlertCaseService/service/FraudInvestigationService.java)

The core method is `processAlertCasePayload(AlertCasePayload)`. Inside one `@Transactional` block:

1. **Save an Alert.** Severity is computed from risk score:
   - `> 80` → HIGH
   - `> 60` → MEDIUM
   - else → LOW
2. **Find or create a CaseCustomer** by customerId (so all cases for one customer roll up).
3. **Save a Case** with status `OPEN`, the alert's ID, reason, decision, amount, customer name, and a link to the CaseCustomer.
4. **Send the customer an email** via `EmailService.sendEmail(...)`. Email body is a full paragraph that says "your transaction was flagged/terminated because: <reason>" plus transaction details. If the email fails the case is NOT rolled back — the failure is logged.
5. **Forward to sarReport.** Build a `ReportingRequest` (case ID, status, risk score, reason, decision, amount, city, state, email, customer ID, account no, balance, name, timestamp) and call `ReportingClient.sendToReporting(req)` which POSTs to `http://localhost:8088/sar/ingest-report`.

### 11.3 Email — [EmailService.java](AlertCaseService/src/main/java/com/cts/AlertCaseService/service/EmailService.java)

Uses **Brevo** (formerly Sendinblue) transactional email API. Credentials come from [application.yml](AlertCaseService/src/main/resources/application.yml) under the `brevo.*` namespace.

```yaml
brevo:
  api-key: ${BREVO_API_KEY:xkeysib-...}
  from: ${BREVO_FROM:bunnypraveen70@gmail.com}
  from-name: ${BREVO_FROM_NAME:BankGuard}
  api-url: ${BREVO_API_URL:https://api.brevo.com/v3/smtp/email}
```

The service POSTs `{ sender, to, subject, textContent }` to Brevo's `/v3/smtp/email` endpoint with the API key in the `api-key` header.

### 11.4 Entities

- **[Alert.java](AlertCaseService/src/main/java/com/cts/AlertCaseService/entity/Alert.java)** — `alertId, severity, createdAt, decisionStatus, riskScore, reason (up to 3000 chars), customerId`.
- **[CaseEntity.java](AlertCaseService/src/main/java/com/cts/AlertCaseService/entity/CaseEntity.java)** — `caseId, alertId, caseStatus, reason, riskScore, decisionStatus, amount, customerName, createdAt, customer (ManyToOne)`.
- **[CaseCustomer.java](AlertCaseService/src/main/java/com/cts/AlertCaseService/entity/CaseCustomer.java)** — groups all cases per customer.

### 11.5 Reporting forwarding — [ReportingClient.java](AlertCaseService/src/main/java/com/cts/AlertCaseService/client/ReportingClient.java)
```java
webClient.post().uri(reportingServiceUrl).bodyValue(request)...block();
```
The `reportingServiceUrl` comes from ConfigServer (typically `http://localhost:8088/sar/ingest-report`).

---

## 12. Deep Dive: sarReport

**Folder:** [sarReport/](sarReport/)
**Port:** 8088
**Purpose:** Stores Suspicious Activity Reports — the regulator-facing record. When AlertCaseService finishes processing a fraud case, it ships the data here for permanent filing and analyst querying.

### 12.1 Controller — [SarController.java](sarReport/src/main/java/com/cts/sarreport/controller/SarController.java) — `/sar`

| Method | URL | What |
|---|---|---|
| `POST /sar/ingest-report` | called by AlertCaseService — receives a `ReportingRequest`, saves it as a SarReport |
| `GET /sar/reports` | list all SAR reports |
| `GET /sar/report/id/{sarId}` | single report by ID |
| `GET /sar/report/name/{customerName}` | by customer name |
| `GET /sar/report/account/{customerAccountNo}` | by account number |
| `GET /sar/report/status/{status}` | by case status |
| `GET /sar/report/city/{city}` | by city |
| `GET /sar/report/state/{state}` | by state |

### 12.2 Service — [SarService.java](sarReport/src/main/java/com/cts/sarreport/service/SarService.java)

`processReportingRequest(ReportingRequest)` is the ingest path. Maps every field from the incoming DTO onto a new `SarReport` entity, stamps `localDate = now`, and saves. The `extractAndMapCustomerPayload` method is currently unused but is plumbing for the case where AlertCaseService might bundle extra enrichment data.

The `generateBy*` methods are straightforward DB lookups; each throws a custom exception if nothing is found, which the `GlobalExceptionHandler` turns into a 404.

### 12.3 Entity — [SarReport.java](sarReport/src/main/java/com/cts/sarreport/entity/SarReport.java)
Fields: `sarId, localDate, caseId, customerId, status, riskScore, reason (3000 chars), amount, city, state, time, customerName, customerEmail, customerAccountNo`.

### 12.4 Config — [application.properties](sarReport/src/main/resources/application.properties)
Minimal — just registers with ConfigServer and Eureka. DB URL and Eureka URL come from ConfigServer's Git repo.

---

## 13. End-to-End: Tracing a Single Transaction

Now let's tie it all together. Say a customer logs in and submits a $5000 transfer to account number `9876543210` from the React frontend.

```
1. Frontend (localhost:5173)
   POST http://localhost:1001/api/transactions
   Headers: Authorization: Bearer eyJ...
   Body: { customerId: 42, amount: 5000, receiverAccountNumber: "9876543210", city: "Bengaluru", state: "KA", ipAddress: "10.0.0.5" }

2. apiGateway (port 1001)
   - JwtAuthenticationFilter validates the token → role = CUSTOMER
   - SecurityConfig: POST /api/transactions/** requires SUPER_ADMIN → 403 ❌
     (Example shows that role enforcement is real. Let's continue with a SUPER_ADMIN call.)
   - With SUPER_ADMIN token: rule passes → gateway forwards to transactionService.

3. transactionService (port 8089)
   TransactionController.createTransaction():
   - Validate sender exists, amount > 0, sender balance >= 5000, receiver account exists.
   - Build Transaction object.
   - Call enrichmentService:
     POST http://localhost:8010/api/enrich/transaction/with-decision-and-alert
     with { currentTransaction, customer, previousTransactions[last 5] }

4. enrichmentService (port 8010)
   EnrichmentService.enrichAndDecideWithConditionalAlert():
   - Enrich the transaction (attach customer + last 5 transactions).
   - Convert to DecisionRequest (combine city+state into location).
   - Call Decision_Engine_service:
     POST http://localhost:7002/api/gemini/analyze-transaction

5. Decision_Engine_service (port 7002)
   GeminiService.analyzeTransactionWithGemini():
   - Load Risk Manager rules from decision_db.
   - Run two heuristic validations (avg amount, location match) → bump risk score.
   - Build a long natural-language prompt for Gemini.
   - Call Google Gemini API.
   - Parse Gemini's JSON reply → { riskScore: 87, decision: "flagged", reason: "Unusual high-value transfer to new account..." }
   - Return that response.

6. Back in enrichmentService:
   - Decision = "flagged" → build AlertCasePayload → call AlertCaseService:
     POST http://alert-case-service/api/investigation/ingest-fraud-alert
     (the hostname is resolved via Eureka to localhost:8085)

7. AlertCaseService (port 8085)
   FraudInvestigationService.processAlertCasePayload():
   - Save Alert (severity=HIGH because risk=87).
   - Find-or-create CaseCustomer for customerId 42.
   - Save Case (status=OPEN).
   - Send email to customer via Brevo: "Your transaction has been flagged..."
   - Build ReportingRequest → call sarReport:
     POST http://localhost:8088/sar/ingest-report

8. sarReport (port 8088)
   SarService.processReportingRequest():
   - Map fields → save a new SarReport.
   - Return the saved entity to AlertCaseService.

9. Response unwinds:
   AlertCaseService → returns 200 OK to enrichmentService.
   enrichmentService → returns TransactionDecisionResponse { decision: "flagged", riskScore: 87, alertSent: true, ... } to transactionService.

10. transactionService:
    - Decision = "flagged" (NOT "terminated") → still moves the money:
      sender balance -= 5000, receiver balance += 5000.
    - Save sender, save receiver, save transaction with status = "flagged".
    - Return 201 CREATED to the gateway.

11. apiGateway streams the response back to the React frontend, which shows
    "Transaction completed but flagged for review."
```

If Gemini had returned **`terminated`** instead of `flagged`, step 10 would
skip the balance update — the transaction is recorded but no money moves.
The customer still gets the alert email.

If Gemini had returned **`genuine`**, step 6 would skip the alert call entirely,
no Case is created, no email is sent, no SAR is filed.

---

## TL;DR — One-Sentence Summary of Each Service

| Service | Sentence |
|---|---|
| **ConfigServer** | Hands every other service its configuration from a Git repo. |
| **EurekaServer** | A phone book — services register here so they can find each other by name. |
| **apiGateway** | The one door the frontend talks to: authenticates with JWT, authorizes by role, forwards to the right backend service. |
| **transactionService** | Manages customers and transactions; orchestrates the fraud-check flow when a transaction is created. |
| **enrichmentService** | Bundles a transaction with its context, asks Gemini for a verdict, and raises an alert if fraud is suspected. |
| **Decision_Engine_service** | Wraps Google Gemini; combines heuristic rules + Risk Manager rules + LLM judgment into a `{ riskScore, decision, reason }`. |
| **AlertCaseService** | Turns a fraud alert into a persistent Case + customer email + downstream SAR filing. |
| **sarReport** | The regulatory record — every flagged/terminated transaction lands here for analyst review. |

---

## Next Steps to Get Comfortable

1. Start everything in the order in [Section 4](#4-boot-order-which-service-to-start-first) and open `http://localhost:8761` to see all services registered with Eureka.
2. Hit `POST http://localhost:1001/auth/register` to make a FRAUD_ANALYST user, then `POST /auth/login` to get a JWT.
3. Use the JWT in Postman to call `GET http://localhost:1001/api/investigation/alerts` — you'll see your request travel: gateway → AlertCaseService.
4. Add `log.info(...)` statements inside each service's controller/service to watch a transaction flow live across terminals.
5. Read [SecurityConfig.java](apiGateway/src/main/java/com/cts/apiGateway/config/SecurityConfig.java) once a day for a week. Once that file feels natural, the whole gateway will feel natural.
