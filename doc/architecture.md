# Backend Architecture

This document describes the architecture implemented in the code, not an idealized target. For operational details on events, security, persistence, testing, and conventions, see [the technical reference](architecture-reference.md).

## Layers and dependencies

The backend is organized into layers and business contexts under `backend/src/main/java/com/boilerplate/`:

```text
com.boilerplate
├── domain/          # model, invariants, events, and repository ports
├── application/     # use cases, commands, queries, DTOs, and outbound ports
├── infrastructure/ # HTTP, security, persistence, messaging, and storage adapters
└── config/          # configuration and bean composition
```

Dependencies point inward:

```text
infrastructure → application → domain
                         config → all layers
```

`domain` must not depend on `application` or `infrastructure`; use cases must not import concrete adapters. `config` wires ports to adapters. ArchUnit checks these rules in `backend/src/test/java/com/boilerplate/architecture/ArchitectureTest.java`.

### Layer responsibilities

| Layer | Responsibility | Examples |
| --- | --- | --- |
| `domain` | Business rules, aggregates, value objects, events, and repository contracts | `domain/orders/Order`, `domain/products/Product`, `Money`, `Sku` |
| `application` | Coordinate use cases, define commands/queries and outbound ports, manage transactions, and return DTOs | `OrderService`, `CreateProductHandler`, `MediaStoragePort` |
| `infrastructure` | Adapt protocols and technologies to use cases or implement their ports | `http/controllers`, JWT filters, messaging adapters, `LocalMediaStorage` |
| `config` | Configure Spring beans and cross-cutting policies | `SecurityConfig`, `JpaAuditingConfig`, `CacheConfig` |

Business packages (`auth`, `customers`, `products`, `orders`, among others) act as bounded contexts. The catalog and orders are reference examples, not dependencies required by a new application.

## Example flow: placing an order

```text
HTTP → OrderController → OrderService → OrderRepository (port)
                                      → Order (rules and invariants)
                                      → Spring Data/JPA persistence
```

`Order` protects aggregate state: it only allows drafts to be edited, rejects empty orders, and derives the total from its lines. `OrderService` loads the required data, coordinates the use case, and saves the aggregate in a transaction. The controller adapts HTTP and returns a DTO; it should contain no business rules and must not expose JPA entities in responses.

For workflows that span aggregates and can fail partially, the fulfillment example uses a saga with steps, recovery, and compensations. Domain events are recorded through the transactional outbox before being published to a transport.

## Hexagonal architecture: ports and adapters

The application defines the contracts it needs; infrastructure provides the implementations:

```text
Application: MediaStoragePort       ← Infrastructure: LocalMediaStorage
Application: PaymentGatewayPort     ← Infrastructure: payment adapter
Application: CommandBus / QueryBus  ← Infrastructure: SpringCommandBus / SpringQueryBus
Domain:      OrderRepository        ← Spring Data JPA generates its implementation
```

HTTP controllers are inbound adapters. JPA, local storage, Kafka, and external providers are outbound adapters. Replacing an adapter should not change domain rules, though it may require different wiring or configuration.

## Pragmatic JPA decision

The domain is independent of the `application` and `infrastructure` packages, but it is **not fully framework-agnostic**: entities use Jakarta Persistence annotations and repository ports use the `org.springframework.data.repository.Repository` marker. This coupling is an accepted trade-off for a straightforward Spring Data JPA boilerplate; it should not be described as persistence-pure separation.

Repositories are small interfaces extending `Repository<T, ID>`, not `JpaRepository`, limiting the operations exposed to the domain. Flyway SQL migrations manage the schema; Hibernate validates it (`ddl-auto: validate`) rather than creating it.

## Design guidelines

- Add abstractions when they protect a rule, boundary, testability, or a realistic evolution path; avoid ceremonial layers without a concrete need.
- Keep controllers thin and DTOs separate from entities.
- Place each invariant in the aggregate or value object capable of enforcing it.
- Reference other aggregates by identity and coordinate cross-aggregate changes in Application or an explicit workflow.
- Update architectural rules and their ArchUnit tests together, documenting any exception.

## Related documentation

- [SOLID, DDD, and hexagonal architecture](solid-ddd-hexagonal.md)
- [Detailed architecture reference](architecture-reference.md)
- [Security baseline](security-baseline.md)
- [README and getting started](../README.md)
