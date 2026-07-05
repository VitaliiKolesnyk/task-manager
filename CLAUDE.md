# CLAUDE.md

Guidance for working in this repository.

## Overview

**task-manager** is a full-stack task CRUD app packaged as a **single deployable
artifact**. A Spring Boot backend serves a REST API *and* the compiled Angular
frontend from one port (`8080`). There is no separate frontend server in
production — Maven builds the Angular app and bundles it into the Boot fat JAR.

- **Backend:** Spring Boot 4.1.0, Java 21, Spring Web MVC + Spring Data JPA
- **Frontend:** Angular 18 (standalone components, `application` builder)
- **Database:** PostgreSQL 16 (via Docker Compose)
- **Build:** Maven (wrapper `mvnw.cmd`), with `frontend-maven-plugin` driving the Angular build

## Project layout

```
pom.xml                         Maven build; wires the Angular build into packaging
docker-compose.yml              PostgreSQL 16 (db=taskmanager, user/pass=postgres, :5432)
src/main/java/com/service/
  TaskManagerApplication.java   Spring Boot entry point
  task/                         Task entity (owner-scoped), controller, repository
  user/                         User entity (@Table "users") + repository
  auth/
    AuthController.java          /api/auth register / login / me (issues JWT)
  security/
    SecurityConfig.java          Filter chain; /api/tasks/** authenticated, else permit-all
    JwtService.java              HMAC JWT generate/validate
    JwtAuthenticationFilter.java Reads Bearer token, populates SecurityContext
    AppUserDetailsService.java   Loads User for Spring Security
  web/
    SpaForwardingController.java Forwards non-file routes to /index.html (Angular SPA)
src/main/resources/
  application.properties        Port, datasource, JPA, jwt.secret/expiration
  static/                       (build output target for the compiled Angular app)
src/main/frontend/              Angular 18 app "task-manager-ui"
  angular.json  package.json
  src/app/
    app.component.ts            Shell: header + logout + <router-outlet>
    app.routes.ts               /login, /register, guarded / (tasks)
    app.config.ts               provideRouter + provideHttpClient(withInterceptors)
    auth/                       AuthService, interceptor, guard, login/register components
    tasks/                      TasksComponent (the task UI, uses signals)
    task.service.ts             HttpClient wrapper for /api/tasks
    task.model.ts               Task + TaskRequest interfaces
src/test/java/com/service/
  TaskManagerApplicationTests.java   Context-load smoke test
  auth/AuthFlowTests.java            MockMvc: 401 unauth, register+token, duplicate 409
```

## How the single-artifact build works

On `mvnw package`, the following runs automatically (all bound to early Maven phases):

1. `frontend-maven-plugin` downloads Node v20.11.1 / npm 10.2.4 (pinned, isolated
   from any system Node), then runs `npm install` and `npm run build`.
2. Angular's `application` builder emits to `src/main/frontend/dist/task-manager-ui/browser`.
3. `maven-resources-plugin` copies that output into `target/classes/static`.
4. `spring-boot-maven-plugin` repackages everything into an executable fat JAR.

Result: `target/task-manager-0.0.1-SNAPSHOT.jar`. Boot serves the Angular app at
`/` and the API at `/api/**` on the same port. `SpaForwardingController` forwards
client-side routes (paths without a `.`) to `index.html` so deep-link refreshes work.

## Common commands

Run all Maven commands from the project root. **Postgres must be running** for the
app to start (it connects on startup).

```powershell
docker compose up -d postgres          # start the database

# Build the single fat JAR (Angular + backend)
.\mvnw.cmd clean package                # add -DskipTests to skip tests

# Run the packaged app  ->  http://localhost:8080
java -jar target\task-manager-0.0.1-SNAPSHOT.jar

# Run from source (also rebuilds the frontend each time — slow)
.\mvnw.cmd spring-boot:run
```

### Fast dev loop (skip the frontend rebuild)

`spring-boot:run` re-runs the full Angular build every time and buffers npm output
(so it looks frozen at "Running 'npm install'" for a few minutes). When you are
only changing backend code, skip the frontend steps — Boot serves the already-built
`static/`:

```powershell
.\mvnw.cmd spring-boot:run "-Dskip.installnodenpm=true" "-Dskip.npm=true"
```

### Frontend live-reload (when working on Angular)

Run the two dev servers separately instead of bundling:

```powershell
.\mvnw.cmd spring-boot:run "-Dskip.installnodenpm=true" "-Dskip.npm=true"   # backend :8080
cd src\main\frontend && npm start                                          # Angular :4200
```

`task.service.ts` calls the API with same-origin relative paths (`/api/tasks`), so
add a proxy (`ng serve --proxy-config`) targeting `http://localhost:8080` if you
develop against `:4200`.

## Authentication

Stateless **JWT** (Bearer token), self-registration. There is no server session — the
frontend stores the token in `localStorage` and an Angular HTTP interceptor
(`src/app/auth/auth.interceptor.ts`) attaches `Authorization: Bearer <token>` to every
request; logout just discards the token client-side.

- Backend flow: `JwtAuthenticationFilter` validates the Bearer token and populates the
  `SecurityContext`; `SecurityConfig` makes `/api/tasks/**` require auth and everything
  else (static assets, `/api/auth/**`, SPA routes) permit-all. Unauthenticated API calls
  return **401** (via `HttpStatusEntryPoint`) so the interceptor can redirect to `/login`.
- **Per-user isolation:** `Task` has an `owner` (`@ManyToOne User`). `TaskController`
  resolves the current user from the `Principal` and every query is owner-scoped
  (`TaskRepository.findByIdAndOwner`, etc.), so users only ever see/modify their own tasks
  — cross-user access returns 404.
- Frontend routing: `/login`, `/register`, and a `authGuard`-protected `/` task view.
  `AppComponent` is a shell (header + logout, shown only when authenticated) with a
  `<router-outlet>`; the task UI lives in `TasksComponent`.
- `owner_id` on `tasks` is **nullable** on purpose — it lets the column be added to a table
  that already holds legacy ownerless rows; new tasks always get an owner, so ownerless
  rows are simply invisible to everyone.
- `jwt.secret` / `jwt.expiration-ms` live in `application.properties`; override the secret
  in production via the `JWT_SECRET` env var.

## REST API

### Auth (`/api/auth`, public)
| Method | Path        | Notes                                                      |
|--------|-------------|------------------------------------------------------------|
| POST   | `/register` | Body `{ username, password }` → `{ token, username }`; 409 if taken |
| POST   | `/login`    | Body `{ username, password }` → `{ token, username }`; 401 on bad creds |
| GET    | `/me`       | Returns `{ username }` for a valid token, else 401         |

### Tasks (`/api/tasks`, requires Bearer token — scoped to the current user)
| Method | Path         | Notes                                                        |
|--------|--------------|-------------------------------------------------------------|
| GET    | `/api/tasks` | Query `?includeDone=true` to include completed tasks; sorted newest-first |
| POST   | `/api/tasks` | Body: `{ title, description }` (owner set from the token)    |
| PUT    | `/api/tasks/{id}` | Partial update; only non-null `title`/`description`/`done` applied; 404 if not owned |
| DELETE | `/api/tasks/{id}` | 204 on success, 404 if missing or not owned            |

## Configuration

`src/main/resources/application.properties`:
- `server.port=8080`
- Datasource: `jdbc:postgresql://localhost:5432/taskmanager`, `postgres`/`postgres`
- `spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-manages the schema (dev convenience; not for prod)
- `spring.jpa.open-in-view=false`

## Conventions & gotchas

- **Lombok** is used on entities (`@Data`, `@RequiredArgsConstructor`); the compiler
  plugin is configured with the Lombok annotation processor.
- Backend lives under `com.service`; feature code is grouped by domain package
  (`task/`), web infrastructure under `web/`.
- Angular uses **standalone components + signals** (no NgModules).
- **Corrupted `node_modules`** (e.g. an interrupted npm install) surfaces as build
  errors like `Could not resolve "@angular/common/http"` or `Could not find a
  declaration file`. Fix with a clean reinstall — do **not** change the pom:
  ```powershell
  cd src\main\frontend
  Remove-Item -Recurse -Force node_modules
  npm ci
  ```
- Ignore `target\*.jar.original` — that's the pre-repackage jar without dependencies.
  Always run the plain `.jar`.
