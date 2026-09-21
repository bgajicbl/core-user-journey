# Core User Journey — MyEdSpace take-home

Mocks the MES core user journey end-to-end: **parent purchases → student onboards → student accesses the LMS.**

## Quick start

```bash
docker compose up
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api

That's it — no other services required. The backend seeds three sample courses (Maths, English, Science) on startup.

### Local dev (without Docker)

Requires a JDK 25 on `PATH`/`JAVA_HOME` and Node 20+.

```bash
# terminal 1
cd backend && mvn spring-boot:run

# terminal 2
cd frontend && npm install && npm run dev
```

Vite's dev server proxies `/api/*` to `localhost:8080` (see `frontend/vite.config.ts`), so the frontend always calls a relative `/api/...` path in both dev and Docker — no environment-specific API URL to configure.

## Architecture overview

```
┌─────────────┐        ┌──────────────┐        ┌────────────────────┐
│   Browser   │──HTTP──▶  nginx (80)   │──/api/─▶  Spring Boot (8080) │
│  React SPA  │        │ frontend cont.│        │   backend cont.     │
└─────────────┘        └──────────────┘        └──────────┬──────────┘
                                                            │ JPA
                                                     ┌──────▼──────┐
                                                     │ H2 (in-mem) │
                                                     └─────────────┘
```

- **Frontend** — React 19 + TypeScript + Vite + React Router. Built to a static bundle and served by nginx, which also reverse-proxies `/api/*` to the backend container. This means the browser only ever talks to one origin, so there's no CORS to configure in production.
- **Backend** — Spring Boot 4 (Java 25), layered as `web` (REST controllers + DTOs) → `domain`/`repository` (JPA entities/Spring Data) → `security` (JWT issuing/validation). Data lives in an H2 in-memory database, seeded with the sample courses/lessons from the brief on startup.
- **Auth** — Deliberately simple: onboarding/login issue a signed JWT (HMAC) identifying the student; the frontend stores it in `localStorage` and sends it as `Authorization: Bearer <token>`. No Spring Security filter chain, no session store — the brief explicitly says a real auth system isn't required, so this stays legible rather than "enterprise." Enforcement is still structural, not a convention every controller has to remember: `AuthInterceptor` is registered against the `/api/lms/**` path pattern in `WebConfig`, so any endpoint added under that prefix is authenticated automatically, and a `@CurrentStudent` argument resolver injects the resolved `Student` into handler methods that need it.

## Core flow, end to end

1. **Parent purchase** (`POST /api/checkout`) — parent picks a course + enters an email (mock checkout, no payment gateway). The backend creates a `Purchase` row with a random invitation token and returns it; the frontend renders it as a shareable `/onboarding/:token` link.
2. **Student onboarding** (`GET`/`POST /api/onboarding/:token`) — the student opens the link, sees the purchased course, and submits their name/email/password. This creates the `Student` record (password hashed with BCrypt), marks the purchase `COMPLETED`, and immediately returns a JWT — the student lands straight in the LMS, no separate login step required.
3. **LMS access** (`GET /api/lms/dashboard`, `GET /api/lms/lessons/:id`) — both endpoints resolve the caller from the bearer token and scope results to that student's purchased course; a lesson from another course returns `403`.
4. **Returning students** use `POST /api/auth/login` (email + password) to get a fresh token.

## Testing

`cd backend && mvn test` runs 14 tests:

- `OnboardingConcurrencyTest` — an integration test (real Spring context, `MockMvc`, the real H2-backed transactional service) covering the onboarding race condition: it fires genuinely concurrent requests at the same invitation token (and, separately, at the same email across different tokens) via a `CyclicBarrier`, and asserts exactly one request wins, every other one gets a clean `409` (never a `500`), and exactly one `Student` is ever committed.
- `JwtServiceTest` — pure unit tests (no Spring context) for token issuance/validation, including tampered tokens and tokens signed with a different secret.
- `CourseControllerTest`, `CheckoutControllerTest`, `AuthControllerTest` — `@WebMvcTest` slices. `AuthControllerTest` specifically asserts (via `Mockito.verify`) that a login with an unknown email still runs a BCrypt comparison — a plain status-code check wouldn't catch a timing side-channel regression, since the vulnerable version also returned `401`.

Not exhaustive — `OnboardingController` and `LmsController` are covered by the integration test rather than their own slice tests, and there's no dedicated test for `AuthInterceptor`/`CurrentStudentArgumentResolver` in isolation.

## Key technical decisions

- **H2 in-memory over Postgres** — the brief asks for pragmatism over completeness within a 3–4 hour scope; H2 gives a real JPA/repository layer with zero extra container, at the cost of resetting data on restart (acceptable for a demo).
- **JWT over a session store** — no server-side session state to manage; validation is a pure function of the token, which keeps `CurrentStudentResolver` (the one thing every protected endpoint calls) trivial to read.
- **nginx reverse proxy instead of CORS** — the frontend never needs to know the backend's hostname; `docker-compose.yml` wires `frontend` → `backend` by Docker's internal DNS, and both dev and prod use the same relative `/api` paths in the frontend code.
- **`open-in-view: false`** with explicit `JOIN FETCH` queries (`PurchaseRepository.findByInvitationToken`, `StudentRepository.findByIdWithCourse`) rather than the default Spring Boot OSIV pattern — avoids the classic hidden-N+1/lazy-init-in-the-view trap while keeping controllers simple.
- **One purchase = one course = one student** — matches the brief's flow exactly; no multi-course cart or multi-child accounts, since that's out of scope for "core journey."
- **Spring Boot 4 / Java 25** — both current as of this writing; picked to show the codebase works against the latest stack rather than defaulting to the last LTS out of caution. This surfaced a real toolchain issue (see AI usage below): Lombok needed to be pinned to an explicit `annotationProcessorPaths` entry in `maven-compiler-plugin`, because implicit classpath-based annotation processor discovery silently stopped generating code under the newer compiler plugin/JDK combination.
- **No CORS configuration at all** — the browser only ever talks to a same-origin server (Vite's dev-server proxy locally, nginx in Docker), so a permissive CORS policy was pure attack-surface with no functional benefit; removed rather than scoped-down.
- **H2 console disabled by default** — nothing in this app depends on it, and leaving a DB console reachable unconditionally is a real misconfiguration class, not a theoretical one.
- **Login timing-safety** — `AuthController` runs a BCrypt comparison (against a precomputed dummy hash) even when the email doesn't exist, so response latency can't be used to enumerate registered accounts.

## AI usage

Built with Claude Code, used both for research and implementation, kept under my review. Also used it to do an architect-level review of the backend, then implement and verify the fixes: structural auth enforcement (`AuthInterceptor` + `@CurrentStudent`), the onboarding race condition, CORS/H2-console hardening, a login timing side-channel, and the added test coverage above — each verified with a real running server (curl, not just reading the diff), including one regression `mvn clean package` caught in my own fix: an overly broad catch-all exception handler that turned Spring's own 404s into 500s.