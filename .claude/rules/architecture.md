description: High-level architecture overview — stack, context layers, and key libraries

## Overview

**task-manager** is a full-stack task CRUD app packaged as a **single deployable
artifact**. A Spring Boot backend serves a REST API *and* the compiled Angular
frontend from one port (`8080`). There is no separate frontend server in
production — Maven builds the Angular app and bundles it into the Boot fat JAR.

- **Backend:** Spring Boot 4.1.0, Java 21, Spring Web MVC + Spring Data JPA
- **Frontend:** Angular 18 (standalone components, `application` builder)
- **Database:** PostgreSQL 16 (via Docker Compose)
- **Build:** Maven (wrapper `mvnw.cmd`), with `frontend-maven-plugin` driving the Angular build
