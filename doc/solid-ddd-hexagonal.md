# SOLID, DDD, and Hexagonal Architecture

This guide explains the backend through existing code examples. Code paths are relative to `backend/src/main/java/com/boilerplate/`.

## DDD in this project

### Bounded contexts

The packages `domain/auth`, `domain/customers`, `domain/products`, `domain/orders`, `domain/api`, `domain/notifications`, and `domain/audit` separate models with distinct vocabularies and responsibilities. They do not all have the same depth: `Product` and `Order` are the main examples of invariant-rich modeling; some areas use simpler models.

### Entities, aggregates, and rules

An aggregate keeps its rules consistent and exposes business-intention methods instead of public setters. For example, `domain/orders/Order`:

- allows lines to be added, changed, or removed only while the order is `DRAFT`;
- rejects non-positive quantities and empty orders when placing them;
- derives its total from its lines;
- records events from domain operations;
- owns its `OrderLine` children.

`domain/products/Product` protects stock and pricing rules. Aggregates coordinate without reaching into one another's state. `OrderService` obtains the customer and product through their repositories and builds the order; longer-running workflows such as fulfillment are coordinated outside the aggregates.

### Value objects and events

`Money`, `Currency`, `Sku`, `Email`, and `Address` represent concepts with their own constraints. Validation in a value object prevents a rule from depending solely on HTTP validation. Events (`OrderPlaced`, `ProductCreated`, and others) express immutable, versioned domain facts.

The outbox records an event in the same transaction as the aggregate change; the relay publishes it after commit. Delivery is at least once, so consumers with side effects must be idempotent. Kafka is an optional transport, not the outbox itself.

### Repositories and transactions

Domain repositories are narrow ports (`OrderRepository extends Repository<Order, UUID>`). Application uses them to load and save aggregates. Write transaction boundaries live in application use cases/services; persistence details remain behind the contract, although the domain uses JPA annotations as a pragmatic trade-off.

## Hexagonal architecture

Hexagonal architecture separates the business core from external mechanisms through ports and adapters:

```text
                 ┌── HTTP / Spring MVC adapter ─────┐
inbound ─────────┤                                  ├── Application ── Domain
                 └── console adapter ──────────────┘        │
                                                             ├── storage port
                                                             ├── payment port
                                                             └── repository port
                                                                   ↑
                                             LocalMediaStorage / gateway / Spring Data
```

- **Inbound ports:** use cases exposed to controllers and other adapters. The typed command/query buses provide a common path for migrated use cases; other services are still called directly as part of the incremental migration.
- **Outbound ports:** interfaces defined inward, such as `MediaStoragePort`, `PaymentGatewayPort`, `PasswordHasher`, and `AuthTokenIssuer`.
- **Adapters:** `LocalMediaStorage`, `SpringPasswordHasher`, Spring buses, HTTP filters/controllers, and event publishers.

The goal is for the core to depend on its own contracts. Infrastructure knows the external technologies and wires them at runtime.

## SOLID

### S — Single Responsibility Principle

Each component should have one primary reason to change:

- `Order` protects aggregate invariants.
- `OrderService` coordinates use cases and transactions.
- `OrderController` adapts the HTTP protocol.
- `OrderRepository` defines the required persistence operations.
- `OrderResponse` defines the output shape.

Avoid placing rules in controllers or mixing persistence and JSON formatting into aggregates.

### O — Open/Closed Principle

Extend behavior through contracts and new adapters where practical, rather than filling use cases with provider conditionals. For example, `EventPublisher` has logging and Kafka implementations selected by configuration/profile; the outbox and aggregates do not need to know about the broker.

This does not mean abstractions are never changed: when a business contract changes, update it deliberately and review its implementations.

### L — Liskov Substitution Principle

An implementation must honor the behavior promised by its port. A payment adapter implementing `PaymentGatewayPort` must return results and report failures according to its contract, just like the in-memory adapter used in tests. Consumer tests should be able to use an alternative implementation without knowing its concrete class.

### I — Interface Segregation Principle

Clients should not depend on operations they do not need. Repositories are aggregate-specific interfaces that declare concrete methods (`findDetailedById`, `save`, etc.) instead of exposing all of `JpaRepository`. Outbound ports should also represent a narrow capability, not an entire external service.

### D — Dependency Inversion Principle

Application policies depend on inward-facing contracts; adapters implement the details:

```text
OrderService     → OrderRepository (domain contract)
Use case         → MediaStoragePort (application contract)
Infrastructure   → Spring/JPA or filesystem implementations
```

Do not inject an `infrastructure` class into an `application` service. Configuration/wiring is where both sides are known.

## DTOs and data flow

DTOs transport data; they do not replace the domain model:

```text
JSON → Controller (Infrastructure) → Command/Query (Application)
     → use case → aggregate and ports → Response DTO → JSON
```

- Commands and queries express application intent.
- `*Response` DTOs expose an explicit projection, not a JPA entity.
- Controllers map HTTP data to the application input model.
- Domain invariants are enforced even when additional validation exists at the HTTP boundary.

Not all current classes have migrated to the same style: some handlers use the command/query bus, while grouped services (`OrderService`, `ProductService`) remain. Adoption is incremental; do not assume every endpoint already uses a bus.

## Explicit boundaries and trade-offs

The current architecture is **pragmatic DDD and partial hexagonal architecture**, not fully framework-independent Clean Architecture:

1. Domain entities are also JPA entities (`jakarta.persistence`).
2. Repository ports use Spring Data types, though they avoid `JpaRepository`.
3. Application uses Spring annotations for transactions and other declarative behavior.
4. Migration to command/query handlers is gradual.

These decisions reduce duplication between the domain and persistence models at the cost of coupling the domain to the ORM. Introduce a separate persistence model if a business rule or persistence need justifies it; there is no need to do so preemptively.

## Practices for adding a context

1. Define the language, entities/aggregates, and rules that must remain invariant.
2. Add value objects where real constraints or semantics exist.
3. Define one repository per aggregate root with only the required operations.
4. Implement the use case in Application and define ports for external dependencies.
5. Add HTTP and outbound adapters in Infrastructure; do not return entities from controllers.
6. Add Flyway migrations; `ddl-auto: validate` should verify the mapping.
7. Test invariants without Spring, use cases with mocked ports, persistence with PostgreSQL/Testcontainers, and layer rules with ArchUnit.

For more detail on structure, events, configuration, security, and testing, see [the technical reference](architecture-reference.md).
