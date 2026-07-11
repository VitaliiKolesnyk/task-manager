---
paths:
  - "src/main/java/com/service/task/TaskRepository.java"
  - "src/main/java/com/service/user/UserRepository.java"
description: Data flow, state management, and db conventions
---

---
paths:
  - "src/main/java/com/service/task/Task.java"
  - "src/main/java/com/service/task/TaskRepository.java"
  - "src/main/java/com/service/user/User.java"
  - "src/main/java/com/service/user/UserRepository.java"
  - "src/main/resources/application.properties"
description: Persistence conventions — JPA entities, repositories, and the PostgreSQL database
---

## Data layer & database

**PostgreSQL 16** accessed through **Spring Data JPA / Hibernate**. Schema is
Hibernate-managed in dev (`spring.jpa.hibernate.ddl-auto=update`) — there is no migration
tool yet. `spring.jpa.open-in-view=false`, so lazy associations resolve only inside a
transaction.

### Entities
- `@Entity` + explicit `@Table(name = "...")`; table names are lowercase plural
  (`tasks`, `users`). Always name `users` explicitly — `user` is a reserved word.
- Primary keys: `Long id` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
  Never assign ids manually.
- Map columns with `@Column`, setting `nullable` / `length` / `updatable` deliberately
  (e.g. `length = 2000` for long text). Use snake_case DB names via
  `@Column(name = "created_at")` with camelCase Java fields.
- Timestamps: immutable `Instant createdAt` set in a `@PrePersist onCreate()` hook
  (`nullable = false, updatable = false`) — don't rely on DB defaults.
- Lombok `@Data @NoArgsConstructor @AllArgsConstructor`, as in `Task`/`User`. Don't let
  `equals`/`hashCode`/`toString` recurse through lazy associations.

### Relationships & fetching
- Default `@ManyToOne`/`@OneToOne` to `fetch = FetchType.LAZY` (JPA defaults them EAGER) —
  see `Task.owner`. With `open-in-view=false`, touch lazy relations only inside a
  `@Transactional` boundary or fetch them per-query with `JOIN FETCH` / `@EntityGraph`.
- Keep relationship graphs out of JSON: `@JsonIgnore` on back-references (as `Task.owner`)
  and expose data via DTOs, never the entity itself.
- Foreign keys via `@JoinColumn(name = "owner_id")`. A nullable FK is a deliberate choice —
  document why in a comment (see the `owner_id` note in `Task`).

### Repositories & queries
- One interface per aggregate extending `JpaRepository<Entity, IdType>`; no `@Repository`
  annotation needed.
- Prefer **derived query methods** for simple reads (`findByOwnerOrderByCreatedAtDesc`,
  `existsByUsername`, `findByIdAndOwner`); encode sorting in the method name (or a `Sort`/
  `Pageable` arg), not in Java.
- Use `@Query` (JPQL preferred) with **named parameters** for anything a derived name can't
  express clearly. Never build queries by string concatenation.
- **Always owner-scope** reads and mutations on user-owned data — filter by the resolved
  `User` (`findByIdAndOwner(id, owner)`) so a foreign/unknown id yields empty → caller
  returns `404`. Never load by id alone and check ownership afterward.
- Return `Optional<T>` for single results and an empty `List<T>` (never `null`) for
  collections. Watch for N+1 queries when iterating lazy relations.

### Transactions & schema
- `@Transactional` on **service** methods for writes / multi-step reads
  (`readOnly = true` where appropriate); keep controllers free of persistence transactions.
- `ddl-auto=update` is dev-only and not production-safe (never drops/renames, can drift).
  If real migrations are adopted (Flyway/Liquibase), switch `ddl-auto` to `validate` and
  version every schema change.
- Datasource credentials in `application.properties` are for local dev only — supply real
  values via environment variables in shared/prod environments.
