# Microservices Architecture

This repository is a microservices-oriented evolution of the Spring + React boilerplate. The current application behavior remains in the Spring monolith under `backend/`, used by the existing React/proxy development stack. Standalone services are extracted incrementally; they are not yet a feature- or security-equivalent replacement.

The architecture references and [Security Baseline](security-baseline.md) are normative.
Do not introduce undocumented exceptions in either backend or frontend; update the
documentation, automated rule, and tests together when an intentional boundary changes.

## Service boundaries

| Service | Owns | Local persistence | Public route through gateway |
| --- | --- | --- | --- |
| `identity-service` | Credentials, users, roles, permissions, access and refresh tokens | `identity-db` | `/api/auth/**` |
| `customer-service` | Customer profiles and addresses | `customer-db` | `/api/customers/**` |
| `catalog-service` | Products, SKUs, prices, and stock | `catalog-db` | `/api/products/**` |
| `order-service` | Orders, order lines, and fulfillment state | `order-db` | `/api/orders/**` |
| `gateway-service` | Routing and the external HTTP entry point; no business data | None | Port 8080 |

Each service owns its schema and database credentials. A service must not query another service's database or share JPA entities. Cross-service data is obtained through an API or represented as a local snapshot/reference owned by the consumer.

## Communication

- **Synchronous HTTP** is for requests that need an immediate answer. The Gateway routes the existing API path prefixes to the relevant service. Service-to-service URLs should be configuration, not hardcoded hostnames.
- **Asynchronous domain events** are for decoupled reactions and workflows. The intended pattern is a transactional outbox in the producing service, Kafka as an optional transport, and idempotent consumers. Do not write to a database and publish directly to Kafka as two uncoordinated operations.
- **Order fulfillment** remains a saga/process manager: payment and stock steps must be idempotent, persist progress, and compensate completed steps when later steps fail.
- **Docker Compose DNS** provides local service discovery. No Eureka/Consul registry is required for the local boilerplate.

## Technology and shared code

Services use Java 21 and Spring Boot 4.1.2. Spring Cloud 2025.1.4 is used for the Spring Cloud Gateway MVC adapter and is aligned with this Boot line. PostgreSQL 18 and Flyway are configured per service. The existing React frontend is retained as the UI.

Avoid a shared library containing domain entities or repositories: it couples deployments and undermines service ownership. Share only stable, versioned wire contracts when there is a demonstrated need; events should be backward-compatible and independently versioned.

## Current status and extraction sequence

The service modules are bootable application shells with independent build files, configuration, and database connections. Business code and the security controls implemented by the monolith have **not yet been fully moved** into these services. The Gateway currently routes paths but has no identity/JWT integration; its security chain therefore denies every business route and allows health probes only. Do not remove this fail-closed behavior or expose the Gateway until the documented identity trust boundary, per-service authorization, and negative contract tests exist. The compose file `compose.microservices.yml` starts the four service databases and builds the service shells. Compose readiness checks indicate that a process (and its database for business services) is ready to respond; they do not indicate feature parity, secure authorization, or production readiness. See the [Security Baseline](security-baseline.md) before exposing either runtime shape.

Recommended extraction order:

1. Move identity/authentication into `identity-service`; issue and validate JWTs with an explicit trust/key-rotation strategy at the gateway and services.
2. Move customers and products into their owners, along with Flyway migrations and tests. Preserve stable IDs and API contracts.
3. Move orders. Replace direct repository access to customer/product state with API calls for request-time data and events/local snapshots where appropriate.
4. Add transactional outbox/inbox and Kafka, then move fulfillment orchestration into an idempotent saga.
5. Add end-to-end contract tests, telemetry aggregation, rate limits, and production secrets/network policies.

Do not dual-write the same production data from the monolith and extracted service. Use a planned migration/cutover per bounded context and keep rollback data until verification.

## Local commands

Build all service modules from the repository root:

```sh
cd backend && ./gradlew buildServices
```

Start the microservices development stack:

```sh
docker compose -f compose.microservices.yml up --build
```

The gateway listens on `http://localhost:8080` bound to loopback only; it denies all business
routes until an authentication and authorization boundary is implemented and must not be
exposed on a network. Each service
exposes Actuator health internally. Local database passwords in the Compose file are
development-only. Set environment overrides before using any shared or production
environment. Loopback binding is defense-in-depth for local development, not a production
security boundary.

## Architecture principles

- Java packages are rooted per service: `com.boilerplate.identity`, `.customers`,
  `.catalog`, `.orders`, or `.gateway`. Keep domain, application, infrastructure, and
  configuration packages inside that namespace; do not create cross-service Java imports.
  The shared Gradle architecture-test source enforces namespace containment, forbids
  dependencies on sibling service namespaces, checks inward layer direction, adapter
  placement, and entity/controller boundaries in each module's own test runtime.
- The monolith pins versioned event names in `DomainEventTypeTest` and representative JSON
  payload fields in `DomainEventWireContractTest`. `OrderFulfillmentSagaIT` exercises the
  persisted end-to-end order workflow and its compensation paths. These tests protect the
  current monolith reference; extracted services still need provider/consumer contract tests
  as their own APIs and event consumers are implemented.
- Keep domain and application dependencies pointing inward within each service.
- Treat each service as an independently deployable bounded context, not merely a package split.
- A service owns its data, migrations, and API/event contracts.
- Prefer eventual consistency for cross-service workflows; do not attempt distributed database transactions.
- Keep transport details out of domain models and make event handlers idempotent.
- Preserve the current boilerplate's ArchUnit, value-object, aggregate, and testing practices within each extracted service.
