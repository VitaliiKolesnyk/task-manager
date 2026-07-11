---
description: Branching, commit message, and pull request conventions for this repository
---

## Git conventions

- **Branch name:** a brief description of the feature (e.g. `add-task-due-dates`,
  `fix-login-redirect`). Use short, hyphenated, lowercase words.
- **Commit message:** a description of the feature or fix (e.g.
  `Add due dates to tasks`, `Fix redirect loop on expired token`). Write it in the
  imperative mood, describing what the change does.

### Pull requests

Every pull request **must** include the following two sections in its description:

- **Description** — what the change does and why. Summarize the feature or fix,
  the motivation behind it, and any notable implementation decisions or trade-offs.
- **Testing plan** — how the change was verified. List the steps taken to test it
  (e.g. `.\mvnw.cmd clean package` runs green, specific manual flows exercised,
  new/updated automated tests) so a reviewer can reproduce the verification.

Use a PR title in the imperative mood, matching the commit-message style above.

`src/main/resources/application.properties`:
- `server.port=8080`
- Datasource: `jdbc:postgresql://localhost:5432/taskmanager`, `postgres`/`postgres`
- `spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-manages the schema (dev convenience; not for prod)
- `spring.jpa.open-in-view=false`