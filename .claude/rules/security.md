---
description: Security and authentication conventions for this repository
---

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
