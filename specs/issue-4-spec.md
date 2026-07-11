# Technical Specification — Issue #4

> **Status: ALREADY RESOLVED (issue CLOSED).** This spec is retrospective/verification-oriented.
> The change was delivered by PR #5 (commit `80157cc`, "Update Node.js from v20.11.1 to v24.18.0").
> The current `master`/working tree already contains the target versions. No further code change is
> required unless a future Node bump is desired.

## 1. Issue Overview

| Field | Value |
|-------|-------|
| Title | Node.js version update |
| Description | Node.js 20 is deprecated; the build's pinned Node should be updated to version 24. |
| Labels | (none) |
| Milestone | (none) |
| State | CLOSED |
| Priority | Low (build-toolchain hygiene; no runtime/behavioral impact) |

## 2. Problem Analysis

The Angular frontend is built during `mvnw package` by the **`frontend-maven-plugin`**, which
downloads a pinned, isolated Node/npm toolchain (independent of any system Node). Prior to the fix,
that pin was **Node `v20.11.1` / npm `10.2.4`**. Node 20 has moved past its active-maintenance window,
so the build toolchain needed to move to the current Node 24 LTS ("Krypton").

Verified facts from the repository:

- The Node/npm versions are declared as Maven properties and consumed by the plugin's
  `install-node-and-npm` goal:
  - `pom.xml:32` — `<node.version>v24.18.0</node.version>`
  - `pom.xml:33` — `<npm.version>11.16.0</npm.version>`
  - `pom.xml:163-164` — `<nodeVersion>${node.version}</nodeVersion>` / `<npmVersion>${npm.version}</npmVersion>`
- Documentation matches: `CLAUDE.md:45-46` states the build downloads **Node v24.18.0 / npm 11.16.0**.
- `frontend/package(-lock).json` `engines.node` constraint (`^18.19.1 || ^20.11.1 || >=22.0.0`)
  is already satisfied by Node 24, so no change was needed there.
- No Dockerfiles or CI workflow files pin a Node version, so `pom.xml` was the single source of truth.

**Root cause of the issue:** a stale version string in `pom.xml` (and its mirror in `CLAUDE.md`).
This is a pure toolchain-version bump — no application code, API, or schema is affected.

## 3. Proposed Solution

The minimal, already-applied solution: bump the two Maven version properties and update the one
documentation reference. No architectural change; no new dependency; the plugin fetches the new
toolchain on the next build.

Trade-offs / reasoning:
- Node **24.18.0** is a real, current Node 24 LTS release with bundled npm **11.16.0** — a clean,
  supported target.
- Keeping the versions as Maven `properties` (rather than inline in the plugin block) preserves the
  existing single-point-of-change pattern; future bumps touch only lines 32–33.

## 4. Step-by-Step Implementation

*(All steps below are already complete on the current branch — listed for traceability/verification.)*

1. **Bump Node property** — `pom.xml:32` set `<node.version>` to `v24.18.0`. ✅ done
2. **Bump npm property** — `pom.xml:33` set `<npm.version>` to `11.16.0` (Node 24's bundled npm). ✅ done
3. **Sync documentation** — `CLAUDE.md:45-46` updated to read "Node v24.18.0 / npm 11.16.0". ✅ done
4. **Confirm no other pins** — grep repo for Node references (Dockerfile, CI, `engines`); none require change. ✅ done

## 5. Verification Strategy

### Unit Tests
- Not applicable — no product code changes; a toolchain-version bump has no unit-testable surface.

### Integration Tests
- Existing `AuthFlowTests` / `TaskManagerApplicationTests` must still pass under a build that uses the
  Node 24 toolchain → confirms the packaged artifact still builds and boots.

### Manual Checks
- Run `.\mvnw.cmd clean package` from the project root → **expected:** `frontend-maven-plugin`
  downloads Node **v24.18.0** / npm **11.16.0**, `npm install` + `npm run build` succeed, and the
  Angular output is bundled into `target/task-manager-0.0.1-SNAPSHOT.jar`.
  > Note: this manual build was **not** executed in the original headless fix run; it should be run
  > locally once to confirm the Angular build succeeds under Node 24 (this is the only open verification gap).
- Launch `java -jar target\task-manager-0.0.1-SNAPSHOT.jar` (Postgres up via `docker compose up -d postgres`)
  → **expected:** app serves the SPA at `/` and API at `/api/**` on port 8080.
- Inspect the plugin download log line → **expected:** it names `v24.18.0`, not `v20.11.1`.

## 6. Files to Modify

| File Path | Nature of Change |
|-----------|------------------|
| `pom.xml` | (Done) Bump `node.version` → `v24.18.0`, `npm.version` → `11.16.0` |
| `CLAUDE.md` | (Done) Update build-doc Node/npm version references |

## 7. New Files to Create

| File Path | Purpose |
|-----------|---------|
| (none) | No new files required |

## 8. Existing Utilities to Leverage

| Utility | Benefit |
|---------|---------|
| `frontend-maven-plugin` (`install-node-and-npm` goal) | Downloads the pinned Node/npm in isolation from system Node — version bump is a one-property change |
| Maven properties `node.version` / `npm.version` | Single source of truth for the toolchain version, consumed by the plugin config |

## 9. Acceptance Criteria

- [x] `pom.xml` pins Node `v24.18.0` and npm `11.16.0`.
- [x] `CLAUDE.md` documentation matches the pinned versions.
- [x] No other Node-version references remain pinned to 20.
- [ ] `.\mvnw.cmd clean package` runs green locally with the Node 24 toolchain (recommended final confirmation).
- [x] No regressions: no application code, API, or DB schema touched.

## 10. Out of Scope

- Upgrading Angular, Java, Spring Boot, or PostgreSQL versions.
- Changing the `engines` constraint in `package.json` / `package-lock.json` (already compatible).
- Introducing Dockerfile or CI-pipeline Node pins.
- Any application/runtime behavior change.