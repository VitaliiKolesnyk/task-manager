---
paths:
  - "src/main/java/com/service/task/TaskController.java"
  - "src/main/java/com/service/auth/AuthController.java"
  - "src/main/java/com/service/web/SpaForwardingController.java"
description: REST api conventions
---

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

