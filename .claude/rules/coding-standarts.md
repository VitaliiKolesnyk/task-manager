---
description: Core coding conventions for the Task Manager codebase
---

# Coding Standards — Spring Boot + Angular

Standards for this repository: a Spring Boot 4 / Java 21 backend and an Angular 18
(standalone components + signals) frontend, packaged as a single deployable artifact.
Follow the existing code in the repo first; these rules fill the gaps.

## General

- **Consistency over preference.** Match the style, naming, and structure of the
  surrounding code. Don't introduce a new pattern when an established one exists.
- **Small, focused changes.** One concern per commit/PR. Keep methods and components short.
- **No dead code.** Remove commented-out blocks and unused imports/variables rather than
  leaving them behind.
- **Fail fast, fail clear.** Validate inputs at the boundary and return meaningful errors.
- **Never commit secrets.** Configuration and credentials come from
  `application.properties` / environment variables (e.g. `JWT_SECRET`), never hard-coded.
- **Format on save.** Keep imports ordered and files free of trailing whitespace.

## Backend (Spring Boot / Java)

### Structure & layering
- Group code by **feature/domain package** (`task/`, `user/`, `auth/`), with cross-cutting
  infrastructure under `security/` and `web/`. Keep controllers thin.
- Layer responsibilities: **Controller** (HTTP, validation, mapping) → **Service**
  (business logic, transactions) → **Repository** (persistence). Controllers must not
  contain business logic or call repositories directly for non-trivial flows.
- Use **constructor injection** (via Lombok `@RequiredArgsConstructor` on `final` fields).
  Never use field injection (`@Autowired` on fields).

### Naming
- Classes `PascalCase`; methods & variables `camelCase`; constants `UPPER_SNAKE_CASE`.
- Suffix by role: `*Controller`, `*Service`, `*Repository`, `*Config`, `*Filter`.
- REST paths are lowercase, plural nouns (`/api/tasks`), no verbs in the path.

### REST & DTOs
- Never expose JPA entities directly over the API. Use **DTOs / records** for requests
  and responses (see `AuthRequest` / `AuthResponse` records).
- Return correct status codes: `200/201` success, `400` validation, `401` unauthenticated,
  `403` forbidden, `404` not found (also for resources the user doesn't own — avoid leaking
  existence), `409` conflict.
- Keep a consistent error body shape (this repo uses an `ErrorResponse(message)` record).
- Validate request bodies at the controller boundary; reject early with a clear message.

### Persistence & entities
- Entities own only persistence concerns. Use Lombok (`@Data`, `@NoArgsConstructor`,
  `@AllArgsConstructor`) as the rest of the codebase does.
- **Owner-scope every query** for user-owned data — resolve the current user from the
  `Principal` and filter by owner (`findByIdAndOwner`, etc.); never trust an id alone.
- Keep `spring.jpa.open-in-view=false`; load what you need explicitly. Be mindful of
  N+1 queries (use fetch joins / `@EntityGraph` when needed).
- Wrap multi-step writes in `@Transactional` at the **service** layer.

### Java language
- Target **Java 21**: prefer `records` for immutable data, `var` for obvious local types,
  enhanced `switch`, and `Optional` for absent values (never return `null` collections —
  return empty).
- Prefer immutability: `final` fields, unmodifiable collections where practical.
- Handle exceptions meaningfully; don't swallow them or catch broad `Exception` without cause.

### Security
- Keep endpoints authenticated by default; open up only what must be public in
  `SecurityConfig`.
- Passwords are always stored hashed (`PasswordEncoder`/BCrypt) — never log or return them.
- Enforce password/policy rules only on the write path (register/change), not on login,
  so existing credentials keep working.

## Data layer & database

The project uses **PostgreSQL 16** accessed through **Spring Data JPA / Hibernate**.
`spring.jpa.hibernate.ddl-auto=update` lets Hibernate manage the schema in dev; there is
no migration tool yet. `spring.jpa.open-in-view=false`, so lazy associations must be
resolved inside a transaction.

### Entities
- Annotate with `@Entity` and an explicit `@Table(name = "...")`. Table names are
  lowercase plural (`tasks`, `users`); `users` is quoted-name-safe because `user` is a
  reserved word — always name it explicitly.
- Primary keys: `Long id` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`
  (Postgres identity/serial). Do not assign ids manually.
- Map columns with `@Column`; set `nullable`, `length`, and `updatable` deliberately
  (e.g. `@Column(length = 2000)` for long text, `updatable = false` for immutable fields).
  Use snake_case DB column names via `@Column(name = "created_at")` while keeping camelCase
  Java fields.
- Timestamps: keep an immutable `Instant createdAt` set in a `@PrePersist` hook
  (`created_at`, `nullable = false, updatable = false`) — follow the existing `onCreate()`
  pattern rather than relying on DB defaults.
- Use Lombok (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`) as in `Task`/`User`.
  Be cautious with `@Data` on entities with associations: avoid `equals`/`hashCode`/
  `toString` recursing through relationships (don't include lazy `@ManyToOne` fields in
  them if you customize).

### Relationships & fetching
- Default `@ManyToOne`/`@OneToOne` to `fetch = FetchType.LAZY` (JPA defaults them to EAGER)
  — see `Task.owner`. Never rely on EAGER to paper over missing queries.
- Because `open-in-view=false`, access lazy relations only within a `@Transactional`
  boundary, or fetch them eagerly per-query with a `JOIN FETCH` / `@EntityGraph`.
- Prevent serializing relationship graphs to JSON: annotate back-references with
  `@JsonIgnore` (as `Task.owner` does) and expose data via DTOs instead.
- Foreign keys use `@JoinColumn(name = "owner_id")`. A nullable FK is a deliberate choice —
  document why in a comment when you use one (see the `owner_id` note in `Task`).

### Repositories & queries
- One repository interface per aggregate, extending `JpaRepository<Entity, IdType>`
  (`TaskRepository`, `UserRepository`). No `@Repository` annotation needed.
- Prefer **derived query methods** for simple reads
  (`findByOwnerOrderByCreatedAtDesc`, `existsByUsername`, `findByIdAndOwner`). Encode
  sorting in the method name or via a `Sort`/`Pageable` argument rather than sorting in Java.
- For anything a derived name can't express clearly, use `@Query` (JPQL preferred over
  native SQL for portability); parameterize with `:names` — **never** build queries by
  string concatenation (SQL-injection / correctness risk).
- **Always owner-scope** queries and mutations on user-owned data: filter by the resolved
  `User` (`findByIdAndOwner(id, owner)`), so a wrong/foreign id yields empty → the caller
  returns `404`. Never load by id alone and check ownership afterward.
- Return `Optional<T>` for single results and empty `List<T>` (never `null`) for
  collections.
- Watch for **N+1 queries** when iterating entities with lazy relations — batch with a
  fetch join or `@EntityGraph`.

### Transactions & schema
- Put `@Transactional` on **service** methods that perform writes or multi-step reads;
  keep read-only operations `@Transactional(readOnly = true)` where a transaction is needed.
- Keep controllers free of persistence transactions.
- Schema is Hibernate-managed (`ddl-auto=update`) for dev only. This is **not** safe for
  production: `update` never drops/renames and can drift. If the project adopts real
  migrations (Flyway/Liquibase), switch `ddl-auto` to `validate` and version every change.
- Datasource credentials live in `application.properties` for local dev only; supply real
  values via environment variables in any shared/prod environment.

## Frontend (Angular / TypeScript)

### Components & architecture
- **Standalone components only** — no NgModules. Declare dependencies in the component's
  `imports`.
- Use **signals** for local state (`signal`, `computed`) and prefer them over ad-hoc
  mutable class fields, matching the existing `TasksComponent` / auth components.
- Keep components presentational where possible; put HTTP and shared logic in
  **injectable services** (`@Injectable({ providedIn: 'root' })`).
- Use `inject()` for dependencies in standalone components/services.

### TypeScript
- `strict` mode assumptions: no implicit `any`, handle `null`/`undefined` explicitly.
- Model API payloads with **interfaces** (`Task`, `TaskRequest`) — don't pass untyped objects.
- Prefer `readonly` for injected deps and signals; avoid mutating inputs.
- Name files `kebab-case` by role: `*.component.ts`, `*.service.ts`, `*.guard.ts`,
  `*.interceptor.ts`, `*.model.ts`.

### HTTP & state
- Call the API through the typed service layer (`task.service.ts`), using **same-origin
  relative paths** (`/api/...`) — the interceptor attaches the auth token.
- Handle errors from observables (`error` callback / `catchError`) and surface a
  user-visible message; don't leave failures silent.
- Unsubscribe or use `takeUntilDestroyed` / the async pipe to avoid leaks.

### Templates & styling
- Keep logic out of templates; compute in the component (signals / `computed`).
- Use semantic HTML and `autocomplete` attributes appropriate to the field
  (`new-password` on registration, `current-password` on login).
- Reuse shared templates/styles carefully — when a template is shared across components,
  guard component-specific UI behind a component field.

## Testing

- **Backend:** cover new endpoints and business rules with MockMvc / `@SpringBootTest`
  integration tests (see `AuthFlowTests`). Test the happy path **and** failure cases
  (validation, auth, not-found/ownership). Use unique fixtures (e.g. `UUID`) to avoid
  cross-test collisions.
- **Frontend:** unit-test services and non-trivial component logic; keep tests deterministic.
- A change to product code should come with a test, or a note on why it can't be tested.
- Run the build/tests before opening a PR (`.\mvnw.cmd package`) — it must be green.

## Git & pull requests

- **Branch names:** short, hyphenated, lowercase, describing the change
  (`add-task-due-dates`, `fix-login-redirect`).
- **Commit messages:** imperative mood describing what the change does
  (`Add due dates to tasks`).
- **Pull requests** must include a **Description** (what & why) and a **Testing plan**
  (how it was verified). Use an imperative-mood title.
