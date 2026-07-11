# CLAUDE.md

Guidance for working in this repository.

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

1. `frontend-maven-plugin` downloads Node v24.18.0 / npm 11.16.0 (pinned, isolated
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
