# Architecture: Technical Reference

Reference for the intended architecture of this boilerplate and the rules that keep it
from drifting. When current code disagrees with this document, the code is the thing that
is wrong. Backend layering is enforced by `ArchitectureTest` (ArchUnit). The
**Resolved seams** section records the DDD deviations that were fixed and how the fix is
kept in place.

Security invariants are specified in the [Security Baseline](security-baseline.md) and are
mandatory for both frontend and backend changes.

Stack: Spring Boot 4.1.2 / Java 21, PostgreSQL 18 + Flyway, JPA/Hibernate, Spring Security
(JWT in httpOnly cookies), Gradle. Frontend: React 19 + TypeScript + Vite.

---

## Layers

`backend/src/main/java/com/boilerplate/` is split into four packages. Dependencies point
**inward only**: `infrastructure` → `application` → `domain`. `config` wires beans and may
see all three.

```
com.boilerplate
├── domain/shared/               # AbstractAggregateRoot, DomainEvent, RecordsEvents
├── domain/{context}/            # the model — only jakarta.* and org.springframework.data.*
│   ├── {Aggregate}.java         # aggregate roots — protect invariants, no @Setter, extend AbstractAggregateRoot
│   ├── {ValueObject}.java       # Email, HashedPassword, Money, Sku, Address …  (validation in the constructor)
│   ├── {Aggregate}Repository.java   # persistence PORT — extends Repository<T,ID>, not JpaRepository
│   ├── event/{X}Happened.java   # domain events — records implementing DomainEvent
│   └── exception/               # DomainException subtypes (shared ones in domain/exception/)
├── application/{context}/       # use cases — orchestration, no business rules
│   ├── {Verb}{X}Command.java    # write request records, implement Command<Result>
│   ├── {X}Query.java            # read request records, implement Query<Result>
│   ├── {Verb}{X}Handler.java    # one write use case, implements CommandHandler, @Transactional
│   ├── {X}Handler.java          # one read use case, implements QueryHandler, readOnly transaction
│   ├── {X}Service.java          # legacy/grouped use cases during incremental bus migration
│   ├── {X}Response.java         # output DTOs (with static from(...) mappers)
│   ├── {X}Result / {X}Principal # use-case outputs that are not a single entity view
│   └── {Capability}.java        # outbound ports: PasswordHasher, CredentialsAuthenticator,
│                                #   AuthTokenIssuer, MediaStoragePort, ImageUpload …
├── application/PagedResponse.java   # shared cross-context response envelope (camelCase JSON)
├── application/dispatch/       # Command/Query contracts, buses and handler ports
├── infrastructure/              # the only layer that knows Spring MVC, Hibernate, JWT, Argon2 …
│   ├── dispatch/                 # SpringCommandBus / SpringQueryBus handler registries
│   ├── http/controllers/        # @RestController — internal /api and external /api/ext
│   ├── http/middleware/         # filters, GlobalExceptionHandler
│   ├── http/AuthCookieWriter    # turns application IssuedTokens into httpOnly cookies
│   ├── security/                # JwtService (AuthTokenIssuer), SecurityUser (UserDetails adapter),
│   │                            #   SpringPasswordHasher, SpringCredentialsAuthenticator, DataInitializer
│   ├── security/                #   + AuthenticationSuccessListener, rate-limit adapters,
│   │                            #     JwtService (opaque refresh tokens)
│   ├── storage/                 # LocalMediaStorage — the MediaStoragePort adapter
│   ├── outbox/                  # OutboxWriter (BEFORE_COMMIT), OutboxRelay (@Scheduled, backoff+DLQ),
│   │                            #   OutboxMaintenance (retention, @SchedulerLock), OutboxAdminService (replay),
│   │                            #   OutboxObservability (metrics + dead-letter health)
│   ├── messaging/               # EventPublisher adapters: LoggingEventPublisher, KafkaEventPublisher,
│   │                            #   KafkaConfig + SecurityEventConsumer (@Profile("kafka"))
│   ├── messaging/inbox/         # InboxGuard — consumer-side dedupe (the outbox's counterpart)
│   ├── messaging/upcast/        # EventUpcaster chain — old event versions lifted to latest
│   ├── saga/                    # OrderFulfillmentSaga: orchestration, compensation, recovery
│   │                            #   (@SchedulerLock), SagaObservability (metrics)
│   ├── payments/                # PaymentGatewayPort: InMemoryPaymentGateway (adapter) +
│   │                            #   ResilientPaymentGateway (@Primary: retry/circuit-breaker/bulkhead)
│   ├── cache/                   # DashboardCacheEvictor — AFTER_COMMIT event-driven @Cacheable eviction
│   ├── http/middleware/         #   + RateLimitFilter (network-key login throttling)
│   └── console/                 # `--spring.profiles.active=cli` commands
└── config/                      # @Configuration, @ConfigurationProperties, bean wiring
                                 #   SchedulingConfig (+ShedLock), JpaAuditingConfig, CacheConfig,
                                 #   ProductionConfigValidator (@Profile prod fail-fast)
```

`{context}` is a bounded context: `auth`, `api` (external API clients), and the
**reference example domain** — a small e-commerce catalog: `customers`, `products`,
`orders` (+ `notifications`, `audit`, `dashboard`). This is the same sample domain the
sibling boilerplates (`boilerplate-symfony-ddd`, `boilerplate-laravel-ddd`) use, so the
three stay interchangeable.

`products` and `orders` are the ones to copy — they exercise every rule:

- **`Product`** — aggregate root with a unique `Sku`, a `Money` price, and stock that can
  never go negative (`reserveStock` refuses to over-reserve, throwing
  `InsufficientStockException` → 422). Price and stock changes each raise a domain event.
- **`Order`** — aggregate root that owns its child entities (`OrderLine`), references
  `Customer` by id, carries an immutable shipping-address snapshot copied from the customer
  at draft time, derives its total from the current lines (never set directly), and only
  allows edits while `DRAFT`. `OrderService.place()` is the cross-aggregate coordination
  point: it reserves stock on every `Product` (each enforcing its own invariant) before
  transitioning the order — neither aggregate reaches into the other.
- **`Customer`** — name, unique `Email`, optional `Address` (composite VO, `@Embeddable`).
- **`Notification`** / **`AuditLog`** — plain entities, no invariants; `AuditLog` rows are
  written by `infrastructure.audit.DomainEventAuditListener` (`BEFORE_COMMIT`, like the
  outbox). **`dashboard`** is a cross-aggregate read model.

`Money`, `Sku` and `Email` are wrapped/unwrapped at the repository boundary and stored as
plain columns — a JPQL `LOWER()` / `LIKE` on a `@Converter`- or `@Embeddable`-mapped VO
attribute maps to `bytea` on PostgreSQL and fails. `Address` is a genuine multi-field
`@Embeddable` (only equality / `IS NULL` on it, never a string function). Delete these
contexts and build your own the same way.

### The dependency rule

| Layer            | May import                                                        | Must NOT import                                             |
|------------------|------------------------------------------------------------------|------------------------------------------------------------|
| `domain`         | JDK, `jakarta.*`, `domain.shared`, its own context; `org.springframework.data.*` (persistence-port vocabulary: `Repository` marker, `@DomainEvents`) | anything else Spring (beans/MVC/Security/transaction), `JpaRepository`, `application`, `infrastructure`, `@Setter` on aggregates |
| `application`    | `domain`, application ports, `org.springframework.transaction`, `org.springframework.cache` (`@Cacheable` — declarative, like `@Transactional`), `org.springframework.data.domain` (pagination), bean-validation, Jackson annotations | `infrastructure`, Spring MVC, `jakarta.servlet`, Hibernate, Spring Security |
| `infrastructure` | everything                                                       | —                                                          |
| `config`         | everything                                                       | —                                                          |

`domain` uses JPA mapping annotations (`@Entity`, `@Column`, `@ManyToOne`, `@Converter`)
— a deliberate "persistence model = domain model" trade-off for a DDD-lite boilerplate.
It does **not** use Spring beans, Spring MVC, or Spring Security types. Repository
*interfaces* may reference `org.springframework.data` (that is the port). If the
persistence trade-off ever blocks a real invariant, split the aggregate from a separate
`infrastructure` persistence entity — but not before.

These rules are enforced by `ArchitectureTest` (ArchUnit). If a change needs to bend one,
change the test in the same commit and say why here.

---

## Resolved seams

The first refactor pass closed these; the rules above and `ArchitectureTest` keep them
closed:

1. **`domain → infrastructure`** — `StringListConverter` now lives in `domain/api/`.
2. **Spring Security out of the domain** — `User` no longer implements `UserDetails`;
   `infrastructure.security.SecurityUser` is the adapter, built with authorities
   materialised so nothing lazy-loads after the transaction closes.
3. **Repository ports** — every `*Repository` extends `Repository<T, ID>` and lists only
   the methods it needs.
4. **Rich aggregates** — `User`, `Role`, `ApiClient`, `Product`, `Order` (+ its
   `OrderLine` children), `Customer` expose intention-revealing methods and factories; no
   `@Setter`. `ArchitectureTest` enforces the no-setter rule.
5. **Typed domain errors** — `BusinessRuleViolationException` (422) and
   `AuthenticationFailedException` (401) replace raw `IllegalArgumentException`;
   `GlobalExceptionHandler` maps `DomainException` and never lets one fall through to 500.
6. **Value objects** — `Email`, `HashedPassword` (`@Converter`), `Money` / `Currency`,
   `Sku`, `Address` (`@Embeddable`), all validating in the constructor. `Sku` / `Email` on
   `Product` / `Customer` are stored as plain columns (VO wrapped at the repo boundary) so
   they stay searchable — see **Domain modelling**.
7. **Outbound ports** — `AuthTokenIssuer`, `PasswordHasher`, `CredentialsAuthenticator`
   (Spring Security / JWT / Argon2 adapters in `infrastructure.security`), and
   `MediaStoragePort` taking a framework-neutral `ImageUpload`. `AuthService` no longer
   touches `HttpServletResponse` — it returns `IssuedTokens`, and `AuthCookieWriter`
   (infrastructure) writes the cookies.
8. **Fetching** — `open-in-view: false`, all `@ManyToMany` are `LAZY`, auth queries use
   `@EntityGraph`, and `hibernate.default_batch_fetch_size` collapses list N+1s.
9. **Refresh sessions belong to the User aggregate.** `RefreshToken` is a child entity
   persisted through `UserRepository`; session creation, rotation, family revocation, and
   account-wide revocation are intention-revealing `User` operations. There is no standalone
   refresh-token repository that bypasses aggregate ownership. Token hashing remains an
   application outbound port with an infrastructure adapter.
10. **Rate limiting is an outbound security capability.** The application defines
    `LoginRateLimiter`; the infrastructure adapter uses a bounded in-memory token bucket for
    local development or an atomic Redis script for distributed deployments. HTTP middleware
    supplies the network key, while the authentication adapter supplies the account key.

11. **Frontend contract.** `services/*-api.ts` types mirror the `*Response` records
   field-for-field; `Paginated<T>` in `lib/api-utils.ts` mirrors `PagedResponse`. No
   `normalize*` translation layer — list params are `page` / `size`. Endpoints that
   return a full collection (`/api/roles`, `/api/permissions`) are consumed as plain
   arrays; those screens are read-only, matching the backend.
12. **Command/query dispatch.** `CommandBus` / `QueryBus` are typed application ports.
    Each handler declares its request type; the Spring adapters resolve exactly one handler
    per request and fail at startup if duplicate registrations exist. Handler methods own
    their transaction boundaries. Product creation and product list/detail are the worked
    migration example; remaining grouped services can move incrementally without changing
    domain contracts or HTTP payloads.
13. **Frontend routing.** Route paths are plain strings on React Router — there is no
    `pathFor()` / route-name registry. The only route file is `app-routes.tsx`.

---

## Pragmatic design

Add an abstraction only when it protects a boundary, a business rule, testability, or a
realistic evolution path. Not because a pattern has a name.

Domain services, specifications, policies, factories, and domain events are all allowed.
Introduce each only when the domain has a concrete need: an invariant that spans
aggregates, a decision rule reused in more than one place, a workflow that needs explicit
coordination. Until then, prefer the smallest correct design.

---

## Domain modelling

- **Aggregates protect their invariants.** State changes go through intention-revealing
  methods (`user.deactivate()`, `apiClient.revoke()`, `apiClient.rotateToken(now)`), not
  setters. Aggregates expose getters; they do not expose `@Setter`.
- **One repository per aggregate root.** Child entities (`Permission` under `Role`,
  `RefreshToken` under `User`, `OrderLine` under `Order`) are loaded and saved through
  their root — `OrderLine` has no repository; it is reached only via `Order`, which uses
  `cascade = ALL, orphanRemoval = true` + `@OrderColumn` so the whole aggregate persists
  in one `save`.
- **Cross-aggregate references are by ID**, not by object. `Order.customerId` and
  `OrderLine.productId` are `UUID`s, not `@ManyToOne`. `RefreshToken` holding a
  `@ManyToOne User` is tolerated only because it is *inside* the `User` aggregate. The
  `orders.customer_id` FK is kept for referential integrity, but the aggregate never
  navigates it.
- **Identity assigned by the domain.** `Customer`, `Product`, `Order` and `OrderLine`
  assign their own `UUID` in the factory (no `@GeneratedValue`), so a creation event
  (`CustomerRegistered`, `ProductCreated`) can carry the id before the row is flushed.
- **Value objects for constrained primitives.** `Email`, `HashedPassword` (`@Converter`),
  `Money`, `Sku`, `Address` (`@Embeddable`). Validation lives in the value object
  constructor, so an aggregate can never hold a malformed value regardless of whether it
  was built by a service, the seeder, or a console command. **A `@Converter`- or
  single-field-`@Embeddable`-mapped VO cannot be used with a JPQL string function**
  (`LOWER`, `LIKE`) — PostgreSQL maps it to `bytea` and the query fails at runtime. So
  `Product.sku` / `Customer.email` are `varchar` columns with the VO wrapped/unwrapped in
  the aggregate (`getSku()` returns `Sku`, the field is `String`); the repository takes
  the raw `String` (`findBySkuValue`, a `default` method bridges from `Sku`). Bean-
  validation on a `Command` DTO is a convenience at the edge, not the enforcement point.
- **`Money`** is integer minor units + an ISO-4217 `Currency`, never a float; arithmetic
  rejects mixed currencies. Stored as two flat columns per amount.
- **Domain events** — an aggregate root extends `domain.shared.AbstractAggregateRoot` and
  calls `recordEvent(new SomethingHappened(...))` from a *semantic method* (`Order.place()`,
  `Product.reserveStock()`), never from a service. Events are immutable records
  implementing `DomainEvent`. See **Events & the outbox** below.

---

## Application layer

- **Command/query bus.** An input record implements `Command<R>` or `Query<R>` and is
  dispatched through the corresponding application port. One `CommandHandler<C,R>` or
  `QueryHandler<Q,R>` declares the request class and handles that use case. The bus is a
  dispatcher, not a mediator for domain rules: handlers still load aggregates through
  repository ports, call domain methods, persist, and map to response DTOs. Do not add
  behavior to the bus itself.
- **Transactions live on handlers.** Command handlers use `@Transactional`; query handlers
  use `@Transactional(readOnly = true)` whenever the response mapper walks lazy data.
  Legacy grouped `*Service` / `*QueryService` classes may remain while migrating contexts
  incrementally. Controllers call only the bus for migrated use cases; direct service
  injection is a transitional pattern, not the target for new use cases.
- A use case that produces something other than one entity view returns a small record
  (`LoginResult`, `IssuedTokens`, `ApiTokenPrincipal`); the controller/filter adapts it.
- With `open-in-view: false` a lazy access outside a transaction throws — that is the point.
- Pagination, search, and UI-shaped projections are read concerns — they must not appear
  as new methods on a domain repository port beyond what a write use case needs.
- **Commands / Queries** are `record` DTOs in the context package. Bean-validation
  annotations catch malformed HTTP input at the edge; the real invariant is enforced in
  the domain (value object constructor or aggregate method).
- **Responses** carry a `static from(Aggregate)` factory. Controllers return `*Response`,
  never an entity (`ArchitectureTest` enforces this).
- **Outbound ports** are interfaces in `application/{context}/`, implemented in
  `infrastructure/`. Current set: `AuthTokenIssuer`, `PasswordHasher`,
  `CredentialsAuthenticator`, `MediaStoragePort` (+ `ImageUpload`, a framework-neutral
  file), `PaymentGatewayPort`. `MediaStoragePort` → `LocalMediaStorage` is the pattern to
  copy for anything that talks to a third-party library or the OS.

---

## Resilience & caching

Two cross-cutting concerns that live in `infrastructure`, never in the domain or the use case.

### Resilience — every outbound call

An adapter that leaves the process (a payment gateway, a webhook, another service) is wrapped
by a **decorator bean** carrying resilience4j annotations — the use case and the domain never
see it.

- **`PaymentGatewayPort`** is the reference. `InMemoryPaymentGateway` is registered as
  `"paymentGatewayAdapter"`; **`ResilientPaymentGateway`** is `@Primary` and decorates it with
  `@Retry` + `@CircuitBreaker` + `@Bulkhead` (execution order
  `Retry ( CircuitBreaker ( Bulkhead ( call ) ) )`). Sustained failure trips the breaker;
  the caller always gets a graceful `declined("gateway_unavailable")` from the fallback,
  never an exception.
- Tuning is the `payment-gateway` instance in `application.yml` (`resilience4j.*`). Metrics
  land on `/actuator/prometheus` as `resilience4j_circuitbreaker_*` / `_retry_*`; an open
  breaker shows `DOWN` in `/actuator/health` (not in the readiness group — page a human,
  don't cycle the pod, same stance as `OutboxObservability`).
- A **timeout** belongs here too. resilience4j's `@TimeLimiter` only wraps
  `CompletableFuture` returns, so a synchronous port sets the timeout on its own HTTP client.
- Swapping in a Stripe/Adyen adapter = register it as `"paymentGatewayAdapter"`; the wrapper
  and the saga do not move.

### Caching — read models with a `@Cacheable` query

- Spring Cache, backed by **Caffeine** in-process (`CacheConfig`). Named caches are declared
  up front so their metrics bind from startup; `app.cache.ttl` / `app.cache.max-size` tune
  every cache.
- The pattern: a query method carries `@Cacheable(...)` (`DashboardQueryService.query()` —
  seven counts across three tables), and an **`AFTER_COMMIT` domain-event listener**
  (`DashboardCacheEvictor`) clears the entry when an event that moves one of its numbers
  commits. `@EnableCaching` runs one notch outside the transaction advisor, so a cache hit
  never opens a DB transaction.
- **Multi-instance:** a Caffeine cache is per-JVM — each instance can serve a slightly
  different value until its own copy is evicted. For a shared cache add
  `spring-boot-starter-data-redis` and return a `RedisCacheManager` from `CacheConfig` under
  a `redis` profile; nothing else changes.

---

## Persistence

- Domain declares the port:

  ```java
  package com.boilerplate.domain.auth;

  import org.springframework.data.repository.Repository;

  public interface UserRepository extends Repository<User, UUID> {
      Optional<User> findByEmail(Email email);
      boolean existsByEmail(Email email);
      User save(User user);
      Optional<User> findById(UUID id);
      Page<User> findAll(Pageable pageable);   // read side; used only by query services
  }
  ```

  Spring Data generates the implementation. No `JpaRepository`, no `CrudRepository`.
- **Eager fetching is explicit.** All `@ManyToMany` associations are `LAZY`. Where a use
  case needs the graph (authentication needs roles + permissions), the port declares a
  dedicated method annotated `@EntityGraph` — e.g. `findDetailedById` /
  `findDetailedByEmail`. `hibernate.default_batch_fetch_size` turns list-view lazy loads
  into a couple of `IN` queries.
- **Custom queries** (`search(...)`) live on the interface with `@Query`, or as a
  `*RepositoryCustom` fragment implemented in `infrastructure/persistence/`.
- **Value objects** map via a `jakarta.persistence.AttributeConverter` that lives in the
  same domain package (`EmailConverter`, `HashedPasswordConverter`, `StringListConverter`)
  — never in infrastructure, since a converter only needs `jakarta.*`.
- **Optimistic locking.** `AbstractAggregateRoot` carries a `@Version` column, so every
  event-emitting aggregate root (`User`, `ApiClient`, `Customer`, `Product`, `Order`) is
  protected against lost updates — see *Configuration, secrets & scaling*.
- **Auditing baseline.** `JpaAuditingConfig` + `domain.shared.AuditableEntity` (a
  `@MappedSuperclass` with `createdAt/updatedAt/createdBy/updatedBy`, filled from the
  authenticated principal or `"system"`). The existing example contexts predate it and keep
  their own `created_at`; **new contexts extend `AuditableEntity`** for consistency.
- **Migrations** are Flyway SQL under `backend/src/main/resources/db/migration/`,
  `V{n}__{snake_case}.sql`, immutable once merged. `ddl-auto` is `validate`. Required
  permissions/roles are initialized idempotently by `DataInitializer`; the first admin
  requires configured credentials if the user table is empty. Example catalog records are
  separate and only seeded by `SampleDataInitializer` under the `dev` profile.
- The DB is the source of truth for schema; entities are mapped to it, not the reverse.

---

## Events & the outbox

```
Aggregate.method()                     domain: recordEvent(new SomethingHappened(...))
   │  service does repository.save(aggregate)
   ▼
Spring Data drains @DomainEvents  ──▶  OutboxWriter  (@TransactionalEventListener BEFORE_COMMIT)
   │                                     writes an outbox_events row  ── same transaction ──┐
   ▼                                                                                        │
COMMIT ◀────────────────────────────────────────────────────────────────────────────────────┘
   │
   ▼  (later, @Scheduled)
OutboxRelay  ──▶  EventPublisher (port)  ──▶  LoggingEventPublisher   (default, no infra)
                                              KafkaEventPublisher     (@Profile("kafka"))
```

- **No dual-write.** The event row and the aggregate change commit together or not at all.
  Nothing is published inside the request transaction.
- **At-least-once.** The relay marks a row published only after `EventPublisher.publish`
  returns; a crash in between means a re-send. **Consumers must be idempotent** (see the
  inbox, below).
- **Backoff, then poison-message handling.** A failed publish parks the row until
  `now + base·2^(attempts-1)` (capped, `app.outbox.backoff.*`) instead of hammering it every
  tick. After `app.outbox.max-attempts` failures the row is dead-lettered (`failed_at` set)
  and skipped — one broken event can't block the queue. `OutboxAdminController`
  (`GET /api/admin/outbox/dead-letters`, `POST …/{id}/replay`, permission `outbox:manage`)
  is the operator surface: replay clears the failure state so the relay picks it up again.
- **Retention.** `OutboxMaintenance` (`@Scheduled`) drops published rows after
  `app.outbox.retention-days`; dead-lettered rows are left for a human. The inbox and saga
  tables have the same sweep.
- **Stable, versioned event types.** `DomainEvent.eventType()` returns a versioned
  identifier (`orders.placed.v1`) — *this*, not the Java class name, is the wire contract.
  `DomainEventTypeTest` pins every one; first segment = aggregate type. Producers only ever
  emit the current version; an old message still on a topic is lifted forward on the way in
  by an **`EventUpcaster`** chain (`infrastructure.messaging.upcast`) — register one
  `@Component` per `vN → vN+1` hop and `EventUpcasterChain` walks them so a consumer only
  ever sees the latest shape (`RefreshTokenReusedV1ToV2Upcaster` is the worked example).
- **Ordering.** `DomainEvent.aggregateId()` is the Kafka message **key**, so every event of
  one aggregate lands on one partition and is delivered in order. The unique per-event id
  (the outbox row id) rides in the `event-id` header and is the consumer's dedupe key —
  key and dedupe id are deliberately different things.
- **Audit trail.** `infrastructure.audit.DomainEventAuditListener` also listens on
  `DomainEvent` `BEFORE_COMMIT` and writes an `audit_logs` row for the events worth a
  permanent record (`customers.registered.v1`, `products.created.v1`,
  `products.price_changed.v1`, `orders.placed.v1`, `orders.cancelled.v1`). The actor comes
  from the request's `SecurityContext`; background work records `"system"`.
- **`AbstractAggregateRoot`** (`domain/shared/`) is the one place the domain touches Spring:
  the `@DomainEvents` / `@AfterDomainEventPublication` annotations, same category as the
  `Repository` marker. The ArchUnit rule `domain_uses_only_spring_data_from_spring` allows
  `org.springframework.data..` and nothing else Spring in the domain.
- **The outbox is always on; the transport is what's pluggable.** Domain events →
  `outbox_events` → `OutboxRelay` runs in *every* profile. The relay calls the
  `EventPublisher` application port; which adapter answers is the only thing that changes:
  - **Default (no `kafka` profile) — `LoggingEventPublisher`** (`@Profile("!kafka")`): logs
    `[event] <aggregate>/<type> <json>`. No broker, no `spring-kafka` beans. This is what
    runs under plain `compose.dev.yml` / in tests / in CI.
  - **`kafka` profile — `KafkaEventPublisher`** (`@Profile("kafka")`): publishes to one
    topic per aggregate (`boilerplate.orders`, `boilerplate.products`,
    `boilerplate.customers`, `boilerplate.auth`, `boilerplate.api`; auto-created), keyed by
    the **aggregate id** (per-aggregate ordering), with the unique event id in an
    `event-id` header and the stable type in an `event-type` header. `KafkaConfig` and the
    `SecurityEventConsumer` example `@KafkaListener` (reacts
    to `auth.refresh_token_reused.v1` on `boilerplate.auth`) are also `@Profile("kafka")`.
- **Why it stays broker-free by default.** `spring-kafka` is on the classpath but
  `KafkaAutoConfiguration` is listed under `spring.autoconfigure.exclude` in
  `application.yml`. With the profile off, no Kafka bean is created and no broker
  connection is attempted — the app boots and every test passes with nothing listening on
  9092.
- **Turning Kafka on** is a Compose override, not a code change:
  ```
  docker compose -f compose.dev.yml -f compose.kafka.yml up -d
  ```
  `compose.kafka.yml` adds a single-node KRaft broker and patches the `backend` service
  with `SPRING_PROFILES_ACTIVE=dev,kafka` + `APP_EVENTS_KAFKA_BOOTSTRAP_SERVERS=kafka:9092`.
  Swapping in a different transport (SNS, RabbitMQ, an HTTP webhook) is a new
  `EventPublisher` bean behind its own profile — the domain, outbox and relay don't move.
- **Inbox — the consumer-side counterpart.** The outbox only promises at-least-once
  delivery, so a real consumer (one that does more than log) must dedupe redeliveries
  itself. `infrastructure.messaging.inbox.InboxGuard.claim(consumer, eventId)` does that:
  an `INSERT ... ON CONFLICT (consumer, event_id) DO NOTHING` into `inbox_processed_events`
  returns `true` only for the delivery that wins the race; every later redelivery of the
  same event id (a Kafka rebalance, an at-least-once retry, a duplicate) claims `false` and
  the caller skips its side effect. Call it inside the same `@Transactional` boundary as the
  side effect it guards — if that write rolls back, the claim rolls back with it, so a
  delivery that genuinely failed is still retried rather than silently marked done.
  `SecurityEventConsumer` is wired this way: it dedupes on the `event-id` header, upcasts
  the payload, then acts. Skipping the guard is a bug for any consumer with a real side
  effect (send an email, call another service, write a row) — only a pure log line can
  safely ignore it.
- Adding an event: a `record X implements DomainEvent` (with a versioned `eventType()` and
  `aggregateId()`) in `domain/{context}/event/`, a `recordEvent(new X(...))` call in the
  aggregate method. Nothing else — the outbox and relay are generic.

### Sagas (long-running, cross-aggregate workflows)

When a use case spans aggregates *and* a step can fail independently (a remote call, a
separate transaction), a single `@Transactional` service method is the wrong tool — it
either holds a transaction open across a network call or leaves half the work done. The
**`OrderFulfillmentSaga`** (`infrastructure.saga`) is the reference:

- `OrderService.place()` now only transitions the order to `PLACED` and emits
  `orders.placed.v1`. It does **not** reserve stock.
- `OrderFulfillmentSagaListener` (`@TransactionalEventListener(AFTER_COMMIT)`) starts the
  saga. It then drives, **one `REQUIRES_NEW` transaction per step**, persisting the new
  `saga_instances.state` before returning:
  `STARTED → PAYMENT_AUTHORIZED → STOCK_RESERVED → COMPLETED`.
- **Compensation** runs the completed steps backwards: a declined payment or a stock
  shortage voids the payment hold (if any) and cancels the order → `COMPENSATED`.
- **Every step is idempotent** so a crash can resume it: payment `authorize` is keyed on
  the saga id (`PaymentGatewayPort` — a port with a throwaway in-memory adapter, the one
  step that leaves the system); stock reservation is folded into the same transaction as
  the `STOCK_RESERVED` transition so a retry never double-reserves; order transitions are
  status-guarded. `SagaRecovery` (`@Scheduled`) re-drives any instance still running and
  untouched for `app.saga.recovery-after-minutes`.
- The saga runs synchronously off the request thread here (simplest, deterministic in
  tests). Switch the listener to `@Async`, or trigger it from the outbox topic under the
  `kafka` profile, if placement latency matters.

---

## HTTP & the two APIs

Both entry points live in `infrastructure/http/controllers/` and call the **same**
application services.

| API            | Path         | Auth                                   | Controllers            |
|----------------|--------------|----------------------------------------|------------------------|
| Internal (SPA) | `/api/**`    | JWT in httpOnly cookie, `JwtAuthFilter`| `UserController`, …    |
| External       | `/api/ext/**`| Client-credentials Bearer, `ApiClientAuthFilter` | `Ext*Controller` |

- Two `SecurityFilterChain` beans, ordered, matched by path prefix (`SecurityConfig`).
- Controllers are thin: validate input, call a service, return a `ResponseEntity<*Response>`.
- Authorization is declarative: `@PreAuthorize("hasAuthority('users:write')")`. Permission
  strings are `{resource}:{action}`.
- **Internal-API JSON is camelCase** (`PagedResponse` is `perPage`, `totalPages`). The
  **external** `/api/ext/auth/*` token payloads are snake_case on purpose — they follow
  the OAuth2 `access_token` / `token_type` / `expires_in` convention (`@JsonProperty`).
- Error handling: one `@RestControllerAdvice` (`GlobalExceptionHandler`), extending Spring's
  `ResponseEntityExceptionHandler` so the framework's own exceptions (unreadable body, type
  mismatch, missing parameter, 404) are covered too. **Every error body is an RFC 7807
  `application/problem+json` document**: `type` (a `urn:problem-type:*` slug), `title`,
  `status`, `detail`, a `traceId` (correlate with logs — see Observability), and for a
  validation failure an `errors` map keyed by field. It maps every `DomainException` subtype
  to a status (`EntityNotFound` → 404, `DuplicateEntity` → 409, `AuthenticationFailed` → 401,
  `BusinessRuleViolation` → 422), turns `OptimisticLockingFailureException` into a 409
  (`urn:problem-type:stale-write`), and has a `DomainException` catch-all so an unmapped one
  is a 400, never a 500. Value-object constructors still reject malformed input with a raw
  `IllegalArgumentException` (a bad SKU / email) — a genuine 400, so it keeps a handler;
  `IllegalStateException` → 409; anything else unhandled is a logged 500.
  `ProblemDetailSecurityHandler` makes the security filter chain
  (401 / 403 before the dispatcher) speak the same dialect. Frontend: `lib/errors.ts`
  (`getErrorMessage`, `getFieldErrors`, `getTraceId`, `isStaleWrite`) is the one place that
  parses it.

### Auth hardening

- **Access token**: short-lived signed JWT, stateless, checked on every request by
  `JwtAuthFilter`. **Refresh token**: an *opaque* random string — only ever a DB lookup
  key, so it carries no claims and can't be forged.
- **Rotation + reuse detection**. A login starts a token *family*; each refresh revokes the
  presented token and issues its successor in the same family. Replaying an already-revoked
  token means it leaked → `AuthService` revokes the whole family, records a
  `RefreshTokenReused` domain event, and returns 401. The breach response runs in
  `REQUIRES_NEW` (`CompromisedRefreshTokenHandler`) so it commits despite the 401.
- **Login rate limiting**: `RateLimitFilter` applies a network-key bucket to
  `/api/auth/login` and `/api/ext/auth/token`; `SpringCredentialsAuthenticator` also applies
  an account-key bucket before password verification. Both use the `LoginRateLimiter` port.
  Local development uses a bounded in-memory adapter; production requires the atomic Redis
  adapter via `distributed-rate-limit`. Exceeded network requests return 429; account bucket
  failures remain generic 401s and do not invoke password hashing.
- **Account lockout**: new account-wide lockouts are intentionally disabled because an
  unauthenticated attacker could lock out a victim. The account-key bucket throttles retries
  without disabling the account. Previously stored, temporary `locked_until` values are
  still honored until expiry; their data is not cleared by deployment. A successful login
  resets legacy failure counters.
- **Response headers**: `SecurityConfig.securityHeaders()` sets a Content-Security-Policy,
  `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY` and HSTS (1 year, `includeSubDomains`
  — only meaningful over HTTPS) on both chains.
- **CSRF protects the browser chain.** The internal chain uses a cookie-backed CSRF token
  repository; `GET /api/auth/csrf` returns a non-cacheable token and the SPA sends it in
  `X-XSRF-TOKEN` on unsafe methods. The token cookie is HttpOnly; the frontend retains the
  response token in memory. The separate stateless `/api/ext/**` bearer/API-client chain
  disables CSRF because it does not authenticate with ambient browser cookies.

---

## Observability

Actuator + Micrometer + OpenTelemetry, wired but quiet by default.

- **Health / probes.** `/actuator/health/liveness` and `/actuator/health/readiness` are
  exposed for Kubernetes (`readiness` = readiness state + DB). The backend `Dockerfile`
  `HEALTHCHECK` hits `readiness`. Details are `when_authorized` (`never` under `prod`).
- **Metrics.** `/actuator/prometheus`. On top of the JVM/HTTP/Hikari defaults the boilerplate
  meters its own moving parts: `outbox.pending`, `outbox.dead_letters`, `outbox.published`,
  `outbox.publish.failed` (`OutboxObservability` / `OutboxRelay`) and `saga.running`,
  `saga.stalled` (`SagaObservability`). A climbing `outbox.dead_letters` or `saga.stalled` is
  the signal something is wedged.
- **Health beyond "is it up".** `OutboxObservability` also contributes an `outbox` health
  entry that goes `DOWN` once the dead-letter count reaches
  `app.outbox.dead-letter-alert-threshold` (default 10). It is deliberately **not** in the
  `liveness`/`readiness` groups — a poison event should page a human, not make Kubernetes
  cycle the pod.
- **Tracing.** `micrometer-tracing-bridge-otel`. `management.tracing.sampling.probability`
  defaults to `0` — trace/span ids still thread through every log line
  (`logging.pattern.level`), nothing is exported. Set it to `0.1`–`1.0` and point
  `management.otlp.tracing.endpoint` at a collector to ship spans. The `traceId` in every
  error body comes from here.
- **Structured logs.** `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` (or `logstash` / `gelf`)
  switches the console appender to JSON — set in `application-prod.yml`, off locally.
- Endpoints `health/**`, `info`, `prometheus` are unauthenticated; lock the management port
  at the network layer (or add auth) if it is reachable from outside the cluster.

## Configuration, secrets & scaling

- **Profiles.** `dev` (verbose logging, sample data) and `prod` (`application-prod.yml`:
  secure `Strict` cookies, JSON logs, `health` details hidden, 10 % trace sampling).
  `ProductionConfigValidator` (`@Profile("prod")`) **refuses to start** if `JWT_SECRET` is
  the dev default or < 32 chars, `COOKIE_SECURE` is false, CORS still points at localhost,
  the distributed rate-limit profile is absent, or Redis is missing/local. A deploy that
  never starts beats one running with an unsafe security configuration.
- **Secrets** are environment variables only (`.env.example` is the manifest; `.env` is
  git-ignored). Nothing sensitive has a usable default in `application.yml`.
- **Every `@Scheduled` job fires on every instance.** The outbox relay is fine with that —
  it claims rows `FOR UPDATE SKIP LOCKED`, so extra instances just add throughput. The
  housekeeping jobs are not: retention sweeps and `SagaRecovery` carry `@SchedulerLock`
  (ShedLock, `shedlock` table, `SchedulingConfig`) so exactly one instance runs each tick.
- **`server.shutdown: graceful`** + `spring.lifecycle.timeout-per-shutdown-phase` — in-flight
  requests and a running relay batch finish before the process exits.
- **Known scaling trade-offs**: local development uses an in-memory login limiter, but
  production requires `distributed-rate-limit` and a non-local Redis endpoint; the Redis
  adapter shares atomic buckets across instances. `JwtAuthFilter` does a `findDetailedById`
  per request (immediate revocation, at the cost of a hot query — cache with a short TTL, or
  carry authorities in the token, if it shows up in profiles).
- **Optimistic locking.** Every event-emitting aggregate root has a `@Version` column
  (`AbstractAggregateRoot`). A stale write throws `OptimisticLockingFailureException` → 409
  — this is what stops the fulfillment saga (background thread) and a user request from
  silently clobbering the same `Order`. For a stale write *across two HTTP round-trips*,
  surface the version as an ETag and check `If-Match`.

### Multi-tenancy

**Out of scope for the boilerplate**, on purpose — the right model (row discriminator vs.
schema-per-tenant vs. database-per-tenant) depends too much on the product. The seam if you
need it: a `TenantContext` (a `ThreadLocal` / request-scoped holder) set from a filter (sub-
domain, header, or JWT claim), then either a Hibernate `@Filter` enabled per request for the
discriminator approach, or Hibernate's `CurrentTenantIdentifierResolver` +
`MultiTenantConnectionProvider` for schema/database isolation. `AuditorAware` already shows
the request-context-to-persistence pattern.

## Frontend

```
frontend/src/
├── services/{context}-api.ts   # one module per backend context: products, customers,
│                               #   orders, dashboard, users, roles, …  axios calls only,
│                               #   types mirror the *Response records field-for-field
├── routes/{context}-route.tsx  # fetches with react-query, hands data to the page
├── pages/{context}/            # screens (index / create / show)
├── app-routes.tsx              # the route table — plain string paths, no pathFor()
├── lib/endpoints.ts            # the URL registry (the one place a path string lives)
├── lib/                        # axios instance, auth, api-utils, helpers
├── hooks/  context/  components/
```

- A `services/*-api.ts` module is the **only** place that knows a URL or response shape
  for its context. Components/pages call services, never `axios` directly. URLs come from
  `lib/endpoints.ts`.
- The service types match the backend `*Response` records exactly. `Paginated<T>`
  (`lib/api-utils.ts`) is the `PagedResponse` shape; list requests send `page` / `size`.
  The customer list adds a `stats` sibling key (`CustomerListPage`). There is **no**
  `normalize*` translation layer — when the shapes disagree, one side is wrong.
- Money is integer minor units + a currency code on the wire; `formatMoney()` in
  `services/products-api.ts` renders it with `Intl.NumberFormat`. Dates: `lib/format.ts`
  (`formatDate` / `formatDateTime`) uses native `Intl` — there is no `dayjs` / `date-fns` /
  `moment` in app code (`react-day-picker` carries its own `date-fns` for the calendar UI).
- Screens for read-only backend resources (`roles`, `permissions`) are read-only. Don't
  add create/edit UI for an endpoint the backend doesn't expose. An `Order` is only
  editable while `DRAFT`, so `orders/show` hides the line controls otherwise.
- Routing is plain React Router: `navigate('/admin/products/' + id)`, not a route
  registry. `app-routes.tsx` is the only file that lists paths.
- **Code-splitting.** Every route module in `app-routes.tsx` is `React.lazy` — the initial
  bundle is the shell + shared vendor only; each screen's JS loads on first navigation.
  `<Suspense>` (in `lib/router.tsx`, one boundary per guarded section) falls back to
  `RoutePending`. `vite.config.ts` `manualChunks` pins the always-loaded vendors
  (`react-vendor`, `radix-vendor`, `tanstack-vendor`) into stable long-cached chunks; heavy
  single-use libs (recharts) land in the route chunk that imports them.
- **Dependencies are earned.** No unused runtime lib sits in `package.json` — an unused
  editor / carousel / signature pad is deleted, not carried (it is install time, audit
  surface, and `.pnpm-store` bloat). `pnpm audit --prod` is expected to be clean.

---

## Architecture enforcement (ArchUnit)

`backend/src/test/java/com/boilerplate/architecture/ArchitectureTest.java` is the
executable copy of this document; `com.tngtech.archunit:archunit-junit5` is a test
dependency and `gradle test` runs it. The rules:

- `layers_are_respected` — Infrastructure accessed only by Config; Application only by
  Infrastructure/Config.
- `domain_does_not_depend_on_application_or_infrastructure`.
- `domain_uses_only_spring_data_from_spring` — the domain may reference
  `org.springframework.data..` (the `Repository` marker, `@DomainEvents`) and nothing else
  from Spring.
- `domain_repositories_are_not_jpa_repositories`.
- `application_is_free_of_web_and_infrastructure` — no `..infrastructure..`,
  `org.springframework.web`, `jakarta.servlet`, `org.hibernate`, or
  `org.springframework.security`.
- `controllers_live_only_in_the_http_package`.
- `entities_have_no_setters` — no method on an `@Entity` matches `set[A-Z].*`.
- `no_domain_entity_is_returned_from_a_controller`.

Changing the architecture means changing a rule in the same commit, with a note in the
**Resolved seams** / dependency-rule sections above.

Existing gates already in `build.gradle` (keep them green, `maxWarnings = 0`):
Checkstyle, SpotBugs (MAX effort), Error Prone, NullAway.

---

## Adding a new bounded context

Copy `orders` + `products` — together they exercise every rule (child entities, a
cross-aggregate coordination in `OrderService.place()`, `Money`, derived totals,
draft-only edits). `auth` is the deeper example.

1. `domain/{context}/` — aggregate root (extend `AssignedIdentityAggregate` if the domain
   assigns the id and emits a creation event, else `AbstractAggregateRoot` +
   `@GeneratedValue`) + child entities, value objects, enum status, `{X}Repository` port
   (extends `Repository<T, ID>` with a `findDetailedById` `@EntityGraph` for the write
   side), `{context}/exception/` if needed. A VO you need to `LOWER()`/`LIKE` in JPQL must
   be a plain column, not `@Converter`/`@Embeddable` (see **Domain modelling**).
2. Flyway migration `V{n}__create_{context}.sql`. App start with `ddl-auto: validate`
   confirms the mapping.
3. `application/{context}/` — `{Verb}{X}Command` records, `{X}Response` / `{X}SummaryResponse`,
   `{X}Service` for writes (`@Transactional`), `{X}QueryService` for reads
   (`@Transactional(readOnly = true)`). Outbound needs → a port.
4. `infrastructure/http/controllers/{X}Controller` (internal, `@PreAuthorize` with
   `{context}:{action}`) and/or `Ext{X}Controller` (external, read-only). Add the
   permissions to `DataInitializer`; demo rows go in a `@Profile("dev")` initializer under
   `infrastructure/bootstrap/`.
5. External API scope: add `{context}:read` to `ApiClientService.ALL_SCOPES` (currently
   `products:read`). The `/api/ext/**` filter chain needs no change (auth is by
   `@PreAuthorize` scope).
6. Frontend: add the URLs to `lib/endpoints.ts`, then `services/{context}-api.ts` (matches
   the `*Response` records field-for-field), `pages/{context}/`,
   `routes/{context}-route.tsx`, register in `app-routes.tsx`, add the nav item to
   `components/layout/data/sidebar-data.ts` + `dynamic-sidebar-data.ts`.
7. Tests: `domain/` unit tests for the aggregate's invariants; `application/` tests with a
   mocked repository port; a `{X}RepositoryIT` if the mapping is non-trivial; ArchUnit and
   the JaCoCo floor run automatically. Extend `AuditableEntity` for the new root if you want
   the `created*/updated*` columns filled for free.

---

## Testing

- `src/test/java/**/domain/` — **pure JUnit**, no Spring context, no DB. Aggregate
  invariants and value objects: `EmailTest`, `HashedPasswordTest`, `MoneyTest`, `SkuTest`,
  `AddressTest`, `UserTest`, `RefreshTokenTest`, `ApiClientTest`, `ApiClientTokenTest`,
  `CustomerTest`, `ProductTest`, `OrderTest` (over-reserve, place-empty, draft-only edits,
  derived total). Fast, the bulk of the suite.
- `src/test/java/**/application/` — service tests with **mocked repository ports**
  (Mockito, no Spring context): `AuthServiceTest`, `UserServiceTest`,
  `ApiClientServiceTest`, `CustomerServiceTest`, `ProductServiceTest`, `OrderServiceTest`.
  They assert the use case wires the domain correctly and translates errors (missing →
  404, duplicate → 409, over-reserve → 422).
- `src/test/java/**/*IT.java` — against a **real PostgreSQL** (Testcontainers, one
  container shared via `support/PostgresTestcontainer`):
  - `AbstractRepositoryIT` (`@DataJpaTest`, Flyway, `ddl-auto=validate`) — `UserRepositoryIT`,
    `CustomerRepositoryIT` (embedded `Address` round-trip, `search`), `ProductRepositoryIT`
    (`Sku` lookup, low-stock count, `Money` round-trip), `OrderRepositoryIT` (line fetch,
    persisted total, draft-cart lookup), `OutboxEventRepositoryIT`.
  - `AbstractIntegrationIT` (`@SpringBootTest` + `MockMvc`) — `AuthControllerIT` (cookies,
    401 without enumeration, 429 rate limit), `ProductControllerIT` (`@PreAuthorize` /
    scope, bad SKU), `OutboxIntegrationIT` (create product → outbox row → relay publishes
    to a captured `EventPublisher`).
- `src/test/java/**/architecture/ArchitectureTest` — the ArchUnit rules above.
- `BoilerplateApplicationTests` — `@SpringBootTest` context load (also on Testcontainers).

The IT suite needs a Docker daemon. `compose.dev.yml` bind-mounts `/var/run/docker.sock`
into the `backend` service and sets `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`, so
`docker compose -f compose.dev.yml run --rm --no-deps backend gradle check` works from a
clean checkout; CI gets Docker from the runner. `@SpringBootTest` ITs are **not**
transactional (the outbox relay runs on its own thread/tx), so their `@DataJpaTest`
siblings assert **deltas**, never absolute row counts.
- Frontend: `tsc --noEmit`, `pnpm lint` (oxlint) and `pnpm test:run` (Vitest) gate every
  change. Vitest runs in `jsdom` with `@testing-library/react` and **MSW** (`src/test/msw/`,
  `onUnhandledRequest: 'error'`); `src/lib/*.test.ts` cover the pure helpers,
  `src/services/products-api.test.ts` exercises axios end-to-end against a mocked endpoint,
  `src/components/InputError.test.tsx` is the render-test template. `make test-frontend`.

### Static analysis / lint (the `phpstan` + `pint` equivalent)

`gradle check` runs all of these; `build.gradle` keeps them at zero-tolerance:

| Tool | Role |
|------|------|
| **SpotBugs** (effort MAX, MEDIUM confidence) | bytecode bug-pattern analysis |
| **Error Prone** + **NullAway** | compile-time bug + null-safety checks |
| **Checkstyle** (`maxWarnings = 0`, severity `error`) | style rules (what Spotless can't express) |
| **Spotless** (`spotlessCheck`, wired into `check`) | formatting — palantir-java-format (4-space, 120 col); `(cd backend && ./gradlew spotlessApply)` / `make format` to fix |
| **ArchUnit** (`ArchitectureTest`) | layer & DDD rules |
| **JaCoCo** (`jacocoTestCoverageVerification`, wired into `check`) | instruction-coverage floor (50 %) — a floor, not a target; raise it, never lower it |

Test sources are linted and formatted too; `config/checkstyle/suppressions.xml` allows
snake_case in `src/test/` (JUnit convention). CI (`.github/workflows/ci.yml`) runs
monolith `gradle check`, optional-service `gradle buildServices` (with the shared per-service
ArchUnit suite), and frontend `tsc` / `lint` / `test` / `build` on every push and PR. The
backend is the single Gradle root project; service scaffolds live under `backend/services/`.
`make check` runs the full backend gate; `make verify` adds the frontend; `make format` runs
`spotlessApply` (the backend equivalent of `pnpm lint:fix`).

Dependency-vulnerability scanning (the `pnpm audit` equivalent) is **Trivy** in CI (Gradle
deps + the image) plus **Renovate** vulnerability alerts — there is no Gradle `audit` task.
Add `org.owasp.dependencycheck` (NVD, a build gate) or `net.ossindex` if a project needs one.

Run everything through Docker per the project memo: `docker compose exec backend gradle test`.

---

## Build & release

- **Backend image: Jib** (`cd backend && ./gradlew jib` to a registry, `jibDockerBuild` for local). No
  Dockerfile — Jib layers the app from the classpath, on `eclipse-temurin:21-jre`, non-root
  (`1000:1000`), multi-arch (amd64/arm64), `MaxRAMPercentage=75`. `IMAGE` /
  `SPRING_PROFILES_ACTIVE` are env vars; tags are `latest` + the project version (CI passes
  `-Djib.to.tags=latest,<git tag>`). The hand-written `infrastructure/backend/Dockerfile` is
  kept only for `docker compose up` convenience — Jib is the canonical prod build.
- **Frontend image: `infrastructure/nginx/Dockerfile.prod`** — multi-stage `pnpm build` →
  `nginx:1.27-alpine` serving the static bundle with an SPA fallback and proxying `/api` to
  `${BACKEND_HOST}:${BACKEND_PORT}` (envsubst on `templates/default.conf.template`). The
  plain `Dockerfile` in that dir is the **dev** edge (proxies `/` to the Vite dev server).
- **SBOM**: `cd backend && ./gradlew cyclonedxBom` → `backend/build/reports/bom.json` (CycloneDX, runtime
  classpath). CI uploads it as an artifact and attaches it to the GitHub Release.
- **Supply chain**: `renovate.json` groups non-major updates, fast-tracks security libs and
  vulnerability alerts, pins GitHub Action + Docker digests. `ci.yml` runs a **Trivy**
  filesystem scan (vuln + misconfig + secret) on every push/PR → SARIF to the Security tab
  (report-only for now; flip `exit-code` to `1` once the backlog is clear).
- **`.github/workflows/release.yml`** (on `v*` tags): build + push both images to GHCR,
  Trivy-scan them, cut a GitHub Release with the SBOM. `make image` / `make sbom` run the
  backend steps locally.
