# Using this repository as a template

## Pick the starting architecture

- **Default for a new product:** start with the implemented modular monolith in `backend/` and the React app in `frontend/`. It contains the working auth, users/roles, sample catalog, customers, and orders implementation.
- **Microservices are optional:** `backend/services/*` and `compose.microservices.yml` are extraction shells. They do not yet provide feature or security parity with the monolith. Use them to explore boundaries, not as the default production runtime.
- Keep only the bounded contexts your product needs. Products/customers/orders are an example domain, not mandatory shared platform functionality.

## Initialize a fresh copy

From the root of a fresh copy, run:

```sh
make init
```

The repository keeps Java source, the Gradle wrapper/build, optional service modules, and the template initializer under `backend/`; `frontend/` remains a sibling application. `renovate.json` stays at the repository root because Renovate discovers repository-wide configuration there. The initializer asks for a display name and a Java base package, then updates Java package declarations/references, Java source directories, Gradle project identifiers, and the visible frontend name. It deliberately skips build outputs, dependency caches, media, and local `.env` files. Review the changes before committing; do not run it against an established application.

You can also provide values non-interactively:

```sh
python3 backend/scripts/init-template.py --name "Inventory Portal" --package com.example.inventory --yes
```

After initialization, copy `.env.example` to `.env`, replace all local credentials, and run the documented verification checks. The development bootstrap account is only for local use. Production must inject a unique first-administrator credential for an empty database through a secret manager; the application refuses the weak development password in production.

## Replace the example domain

The monolith includes a sample e-commerce bounded context. Treat its migrations, DTOs, APIs, permissions, sample records, frontend routes, and tests as a cohesive example to replace or remove—not as platform infrastructure. Keep authentication, authorization, auditing, observability, error handling, and deployment conventions that fit the new product. Update architecture/security docs and tests alongside any boundary changes.

Before enabling a service module, implement its API, authorization, persistence migrations, contract tests, readiness behavior, and operational ownership. Do not expose the current service shells as a production replacement for the monolith.
