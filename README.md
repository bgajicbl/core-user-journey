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
- **Auth** — Deliberately simple: onboarding/login issue a signed JWT (HMAC) identifying the student; the frontend stores it in `localStorage` and sends it as `Authorization: Bearer <token>` to the two LMS endpoints. No Spring Security filter chain, no session store — the brief explicitly says a real auth system isn't required, so this stays legible rather than "enterprise."

## Core flow, end to end

1. **Parent purchase** (`POST /api/checkout`) — parent picks a course + enters an email (mock checkout, no payment gateway). The backend creates a `Purchase` row with a random invitation token and returns it; the frontend renders it as a shareable `/onboarding/:token` link.
2. **Student onboarding** (`GET`/`POST /api/onboarding/:token`) — the student opens the link, sees the purchased course, and submits their name/email/password. This creates the `Student` record (password hashed with BCrypt), marks the purchase `COMPLETED`, and immediately returns a JWT — the student lands straight in the LMS, no separate login step required.
3. **LMS access** (`GET /api/lms/dashboard`, `GET /api/lms/lessons/:id`) — both endpoints resolve the caller from the bearer token and scope results to that student's purchased course; a lesson from another course returns `403`.
4. **Returning students** use `POST /api/auth/login` (email + password) to get a fresh token.

## Testing

`cd backend && mvn test` runs `OnboardingConcurrencyTest`, an integration test (real Spring context, `MockMvc`, the real H2-backed transactional service) covering the onboarding race condition described below: it fires genuinely concurrent requests at the same invitation token (and, separately, at the same email across different tokens) via a `CyclicBarrier`, and asserts exactly one request wins, every other one gets a clean `409` (never a `500`), and exactly one `Student` is ever committed. It's currently the only automated test in the project — everything else was verified manually end-to-end (see AI usage below); a real codebase at this stage would also want `@WebMvcTest` slices per controller and a couple of `JwtService`/`CurrentStudentResolver` unit tests.

## Key technical decisions

- **H2 in-memory over Postgres** — the brief asks for pragmatism over completeness within a 3–4 hour scope; H2 gives a real JPA/repository layer with zero extra container, at the cost of resetting data on restart (acceptable for a demo).
- **JWT over a session store** — no server-side session state to manage; validation is a pure function of the token, which keeps `CurrentStudentResolver` (the one thing every protected endpoint calls) trivial to read.
- **nginx reverse proxy instead of CORS** — the frontend never needs to know the backend's hostname; `docker-compose.yml` wires `frontend` → `backend` by Docker's internal DNS, and both dev and prod use the same relative `/api` paths in the frontend code.
- **`open-in-view: false`** with explicit `JOIN FETCH` queries (`PurchaseRepository.findByInvitationToken`, `StudentRepository.findByIdWithCourse`) rather than the default Spring Boot OSIV pattern — avoids the classic hidden-N+1/lazy-init-in-the-view trap while keeping controllers simple.
- **One purchase = one course = one student** — matches the brief's flow exactly; no multi-course cart or multi-child accounts, since that's out of scope for "core journey."
- **Spring Boot 4 / Java 25** — both current as of this writing; picked to show the codebase works against the latest stack rather than defaulting to the last LTS out of caution. This surfaced a real toolchain issue (see AI usage below): Lombok needed to be pinned to an explicit `annotationProcessorPaths` entry in `maven-compiler-plugin`, because implicit classpath-based annotation processor discovery silently stopped generating code under the newer compiler plugin/JDK combination.

## AI usage

Built with Claude Code, used both for research and implementation, kept under my review throughout:

- Used Claude's browser automation to open the Ashby assignment page directly (`GET /api/courses` in the brief-provided sample data was mirrored verbatim into `DataSeeder`) rather than working from a paraphrase.
- Talked through three architecture decisions explicitly before writing code — repo layout (`backend/` + `frontend/` vs. extending the existing root scaffold), persistence (H2 vs. Postgres), and auth (opaque token vs. JWT) — I made the calls, Claude implemented them.
- Claude wrote the Spring Boot backend (entities, repositories, controllers, JWT service, exception handling) and the React frontend (routing, pages, auth context, API client) directly.
- Verified the build at each stage rather than assuming it worked: `mvn compile`, `tsc -b`, `vite build`, then actually ran both dev servers and drove the full purchase → onboarding → LMS → logout → login journey through a real browser session. That caught a real bug — a `LazyInitializationException` from `open-in-view: false` on `purchase.getCourse()` — which was fixed with the `JOIN FETCH` queries described above, then re-verified in-browser.
- Same verification loop was repeated against the actual Docker Compose stack (not just the images building, but `docker compose up` + curling both the direct backend and the nginx-proxied frontend, then a final browser pass against `localhost:3000`) before calling it done.
- When asked to move the backend to Spring Boot 4 / Java 25: installed a local JDK 25 (`brew install openjdk@25`, since the cask installer needed a sudo prompt this environment couldn't supply, so the keg-only formula was used instead), bumped the parent POM and Docker base images, then re-ran the same verification loop. `mvn compile` initially looked fine, but that was a false negative from stale incremental class files left over from the pre-upgrade build — a fresh `mvn clean compile` (and the Docker build, which always compiles from scratch) exposed that Lombok's implicit annotation-processor discovery had silently stopped generating getters/setters/builders under the newer `maven-compiler-plugin`/JDK 25 combination. Bumping the Lombok version didn't fix it; explicitly pinning Lombok onto `annotationProcessorPaths` did. Re-verified with a clean local build, the Docker image build, and the full purchase → onboarding → LMS journey through the dockerized stack.
- Asked for an architect-level review of the backend: found (among other things) that `OnboardingController.completeOnboarding` had a check-then-act race with no transactional boundary — two concurrent requests for the same invitation token, or the same email across two different tokens, could both pass validation before either committed, and the loser surfaced as a raw, unhandled `500` once it hit the DB's unique-constraint rejection. Fixed with `@Transactional` plus a `DataIntegrityViolationException → 409` mapping in `GlobalExceptionHandler`.
- Asked to add tests for that race: writing them surfaced that Spring Boot 4 restructured the test starters — `TestRestTemplate` and `@AutoConfigureMockMvc` no longer ship in `spring-boot-starter-test`. The first fix tried, swapping in `spring-boot-starter-test-classic` (Spring's official "restore 3.x behavior" bundle), pulled in `spring-boot-security-test` and broke context loading, because this project uses `spring-security-crypto` (just for BCrypt) but not full Spring Security. Settled on adding the narrower `spring-boot-starter-webmvc-test` module directly. Proved the resulting `OnboardingConcurrencyTest` was a real regression test — not just a test that happened to pass — by checking out the pre-fix `OnboardingController`/`GlobalExceptionHandler` from git history, confirming both concurrency tests failed with unhandled `500`s against that code, then restoring the fix and confirming green (5 consecutive runs, no flakiness) plus a full `mvn clean package` and Docker rebuild.
