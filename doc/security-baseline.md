# Security Baseline

Mandatory security constraints for this repository **as it is currently structured**. This
document complements, and does not replace, the architecture rules in
[`architecture-reference.md`](architecture-reference.md) and
[`microservices-architecture.md`](microservices-architecture.md). If a code change needs
an exception, the exception must be explicit, scoped, tested, and documented in the same
change; convenience is not a reason to weaken a control.

## Current application shape and security status

There are two distinct runtime shapes; they are not yet security-equivalent:

1. **Implemented application:** the React/Vite SPA is served by the frontend container in
   development and by Nginx in the production image. Nginx is the public edge and proxies
   `/api/` to the Spring monolith in `backend/`; that backend owns the current application
   behavior and one PostgreSQL database. `backend/` is therefore the security reference
   implementation for existing features.
2. **Microservices extraction:** `backend/services/*` and `compose.microservices.yml` contain
   bootable service shells and separate PostgreSQL databases. Business behavior has not
   been fully extracted. The Gateway currently routes paths; it does not replace backend
   authentication/authorization. Do not treat this stack as production-ready or assume it
   inherits the monolith's security filters, policies, or tests. Each extracted service and
   the Gateway must implement and test the controls relevant to their trust boundary before
   exposure.

Current controls in the monolith include Argon2 password hashing; short-lived access JWTs
and opaque, rotated refresh tokens; `HttpOnly` cookies; lockout and per-process login rate
limiting; method-level authorization; security response headers; and a production
configuration validator. These controls are not proof that every API path or deployment is
safe: retain their tests and verify the edge/deployment configuration too.

One major gap remains: the extracted Gateway is routing-only with no
authentication/authorization filter. Routing is not authorization. The production Nginx
template now adds security headers to static SPA/media responses, and CI validates its
rendered configuration.

The frontend defaults to `SpringHttpOnlyAdapter`, aligned with the monolith's login, current
user, refresh, and logout routes. Spring user responses include roles and permissions for
the UI's navigation/access hints; authorization remains enforced by the backend. Adapter
mapping tests and backend login/cookie integration tests cover these contracts. The
browser-authenticated monolith chain requires a CSRF token; the stateless external-client
chain remains CSRF-disabled because it uses explicit bearer/API credentials, not ambient
browser cookies. Keep cookie, SameSite, CORS, and origin policy under review before public
exposure.

## 1. Security principles

- Default-deny: endpoints and data are private unless explicitly authorized otherwise.
- Enforce authorization on the server for every operation and resource. Frontend route
  guards and hidden controls are UX only, never an access-control boundary.
- Minimize privileges, exposed data, dependencies, network access, and secret lifetime.
- Fail closed on invalid, missing, or ambiguous security configuration.
- Never log credentials, access/refresh tokens, session cookies, API secrets, or sensitive
  personal data. Security events should be auditable without storing secrets.
- Treat data from HTTP, messages, files, environment variables, and other services as
  untrusted input.

## 2. Identity, sessions, and authorization

- Passwords are stored only as adaptive one-way hashes using the configured Spring
  Security Argon2 encoder. Never store or log plaintext passwords; never implement custom
  cryptography.
- The monolith issues `access_token` (`Path=/`) and `refresh_token`
  (`Path=/api/auth/refresh`) cookies; both are `HttpOnly`, with `Secure` and `SameSite`
  controlled by Spring configuration. Production must keep `Secure=true` and the configured
  production `SameSite` policy. Preserve the narrower refresh-cookie path. Cookie domain,
  expiry, and access/refresh lifetimes must be deliberate. JavaScript must not read or persist
  bearer/refresh tokens in `localStorage`, `sessionStorage`, IndexedDB, or URLs.
- Access credentials are short-lived. Refresh credentials are opaque, high-entropy,
  server-verifiable, rotated on use, and revoked on logout/reuse detection. Never put
  secrets or unnecessary personal data in JWT claims.
- Every protected backend operation requires an explicit authenticated principal and
  permission/scope check. Check ownership/tenant boundaries in the use case, not only in
  the UI or at route level. Add negative tests for unauthenticated and unauthorized access.
- Keep internal-user and external-client authentication paths separate. External client
  credentials are scoped, revocable, stored as non-recoverable hashes where applicable,
  and returned only once at issuance.
- The monolith's rate limiter and account lockout are per-process, not cluster-wide.
  Distributed deployments must use a shared limiter/store before claiming a global limit.
  Generic authentication failures must avoid account enumeration.

## 3. Browser, HTTP, and API boundaries

- CORS is an explicit allowlist of trusted origins. Never use wildcard origins with
  credentials. Production configuration must reject local development origins.
- The internal browser-authenticated chain requires a CSRF token for every unsafe method.
  The SPA obtains a token from `GET /api/auth/csrf` and sends it in `X-XSRF-TOKEN`; the
  server-managed `XSRF-TOKEN` cookie is HttpOnly and its `SameSite`/`Secure` flags follow
  the cookie policy. Keep CORS origins explicit and test that missing tokens are rejected.
  The stateless `/api/ext/**` bearer/API-client chain is separate and CSRF-disabled. Reassess
  whenever origins, proxy topology, cookie policy, or authentication flows change.
- The Spring monolith sets CSP, HSTS, `X-Frame-Options: DENY`, and
  `Referrer-Policy: no-referrer`. Its current CSP allows `unsafe-inline` for scripts and
  styles. Do not broaden it or add `unsafe-eval`; remove inline-script allowance when the
  SPA/proxy setup permits, or document the exact need and a nonce/hash migration plan.
  Production Nginx applies CSP and browser-hardening headers to static SPA/media responses;
  API responses retain Spring headers. CI renders and validates the Nginx template.
- Validate input at the HTTP boundary and enforce business invariants again in the domain.
  Use parameterized queries; never build SQL/JPQL from untrusted string concatenation.
- Return stable problem responses without stack traces, SQL, filesystem paths, secrets, or
  internal topology. Include a correlation/trace identifier, not sensitive context.
- The monolith exposes selected Actuator health/info/Prometheus and OpenAPI/Swagger paths;
  the production Nginx template also proxies Swagger paths. Keep these endpoints minimal
  and restrict them at the edge/network or require authentication if they are not intended
  to be public. The microservice shells expose health/info/Prometheus internally; do not
  publish them without a reviewed need.
- Configure upload size/type limits, validate actual content (not only file extensions or
  client MIME types), generate storage names, and prevent path traversal. Do not serve
  uploaded content as executable same-origin content.

## 4. Frontend constraints

- Use the shared API client and endpoint registry (`lib/axios.ts`, `lib/endpoints.ts`) and
  context API modules. Do not add direct ad-hoc HTTP calls that bypass credentials,
  interceptors, or common error handling.
- Keep authorization decisions on the backend. Frontend permissions may hide actions but
  cannot grant access or be trusted as proof of authorization.
- The frontend auth store holds display/session state, not bearer tokens. Preserve
  `withCredentials` in the shared API client and keep auth cookies inaccessible to
  JavaScript. The Spring adapter keeps the CSRF token in memory and sends it only as the
  `X-XSRF-TOKEN` header on unsafe requests.
- Do not render untrusted content as HTML (`dangerouslySetInnerHTML`) without a reviewed
  sanitization strategy. Avoid unsafe URL schemes and validate redirect destinations.
- Do not put secrets in `VITE_*` variables: they are compiled into public browser assets.
  Frontend configuration may contain public endpoints and non-sensitive feature settings
  only.
- Keep dependencies purposeful and lockfile-pinned. Run `pnpm audit --prod` in the
  frontend container/CI; evaluate advisories and transitive dependency impact before
  accepting exceptions. Current CI does not run this command explicitly; Trivy scans the
  lockfile instead.

## 5. Backend, data, and service boundaries

- Preserve the monolith's layer rules and the target service ownership documented in the
  architecture references. The extracted services currently have separate Compose
  databases, but their shells do not yet implement full domain/security behavior. A service
  must never read or write another service's database; cross-service communication uses
  authenticated APIs or versioned events once those flows are implemented.
- Use least-privilege database users, parameterized access, Flyway migrations, and
  `ddl-auto: validate`. Never use schema auto-creation as a production migration strategy.
- Do not expose persistence entities directly. Response DTOs must contain only fields the
  caller is authorized to see; avoid mass assignment by mapping accepted fields explicitly.
- For events and remote calls, use the documented outbox/inbox/idempotency and resilience
  patterns. Do not dual-write a database and a broker. Redact sensitive event payloads and
  define retention for audit/event data.
- Restrict service and management ports to the required network. Use TLS for production
  service-to-service and database connections where the deployment requires it. Do not
  publish internal service/database ports by default in Compose.

## 6. Secrets, configuration, and runtime

- Secrets come from a managed secret store or injected runtime environment, never source,
  Docker build arguments, frontend variables, or image layers. `.env.example` contains
  names/placeholders only; real `.env` files stay ignored. Current Compose files have
  development-only database password defaults for microservices; never reuse them outside
  local development. The monolith also has development defaults in `application.yml`; only
  its `prod` profile validator is a fail-fast safeguard, so production must activate `prod`
  and provide explicit secrets.
- Production startup must fail for missing/weak signing keys, insecure cookies, unsafe CORS,
  development credentials, or other explicitly unsafe settings. Keys must be randomly
  generated, sufficiently strong, rotatable, and different per environment.
- The first administrator is created only when the user table is empty and explicit
  `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` values are available. Production
  bootstrap passwords must contain at least 20 characters, must be injected from a secret
  manager, and must be removed after the account is provisioned. Never log the password;
  `admin123` is a local-dev/test fixture only.
- The canonical backend Jib image runs non-root; do not assume every Compose/dev image has
  the same hardening. Production containers must run non-root, use patched base images,
  pin images by digest where practical, and avoid privileged mode or unnecessary
  capabilities/mounts. The dev backend mounts the Docker socket for Testcontainers; this
  privilege is development/test-only and must not be copied into production.
- Apply graceful shutdown, bounded resource limits, request/body/time limits, and safe
  error handling. Do not expose debug tooling or development profiles in production.
- Backups containing personal or business data must be access-controlled, encrypted, and
  covered by retention and restore procedures.

## 7. Supply chain and security gates

- Commit lockfiles for Gradle and pnpm; CI must use frozen/locked dependency installation.
- Review dependency changes for maintenance, license, provenance, and known vulnerabilities.
  Generate the backend SBOM and scan source/dependencies/images in CI.
- Security scans must produce an actionable result. High/Critical findings block release
  unless a time-bounded, owner-assigned risk acceptance is recorded; informational reports
  with a permanently successful exit code are not a security gate.
- Keep CI actions pinned to immutable commit SHAs where supported, grant least-privilege
  workflow permissions, and never expose repository secrets to untrusted pull-request code.
- Never lower a test, coverage, lint, or security threshold to make a change pass. Fix the
  issue or record a narrowly scoped exception with an expiry/review date.

## 8. Required checks for security-sensitive changes

At minimum, tests must cover the relevant negative paths:

- access without authentication, with insufficient permission, and across resource-owner
  boundaries;
- login throttling, generic failures, refresh rotation/reuse, logout/revocation, and cookie
  flags;
- CORS/CSRF behavior for browser-authenticated writes;
- validation and malformed input, including upload limits when uploads exist;
- secret/configuration fail-fast behavior and safe error/log output;
- event consumer idempotency and redaction when messages carry sensitive data.

The repository's CI and release validation run the consolidated backend `check`, optional
service `buildServices` (including the per-service ArchUnit suite), frontend
TypeScript/lint/tests/build, SBOM generation in CI, and Trivy filesystem scans. High/Critical
Trivy findings fail both CI and release validation; image scans also fail a release for
High/Critical findings. There is no explicit `pnpm audit` command; Trivy scans the lockfile.
See
`.github/workflows/ci.yml`.

## 9. Change review and architecture drift

Before merging a feature or infrastructure change:

1. Identify the affected bounded context, API/event contract, data classification, and
   trust boundary.
2. Keep backend/frontend contracts aligned with the architecture reference; update both
   sides and their contract tests together.
3. Add or update executable architecture/security tests for any new invariant or boundary.
4. Update this baseline and the relevant architecture document in the same change if a
   policy or deliberate exception changes. Explain why, scope it, and record its review
   date; never silently bypass ArchUnit, lint, or CI.
5. Run the documented backend and frontend gates. A locally passing build does not replace
   CI or a security review for authentication, authorization, cryptography, data exposure,
   deployment, or dependency changes.

The architecture documentation is normative: implementation drift is treated as a defect.
Any intentional architecture change requires an explicit review and synchronized updates
to documentation, automated rules, and tests.
