# Staff Engagement POC — Project Overview

## Purpose
A system to centralise staff interactions (check-ins, mentoring, catch-ups), turn them into follow-up tasks, and maintain a skills register across the organisation.

## Architecture
- Modular monolith (single deployable, module boundaries enforced by package structure)
- Backend: Java 21, Spring Boot 3.4.x
- Frontend: Angular 21, standalone components
- Database: PostgreSQL
- Communication: REST API (frontend → backend)
- Containerisation: Docker / Docker Compose

## Domain Modules
Each module lives in its own top-level package under `com.staffengagement`:
- `employee` — central person record
- `staff` — staff members who use the system (log interactions, create tasks, etc.)
- `interaction` — notes/records of engagements linked to an employee
- `task` — follow-up actions spawned from interactions, centralised per person
- `portfolio` — skills, education, projects, public links per employee
- `skills` — skills register: quantifies experience (years + project count) per skill

Modules communicate via public service interfaces, not by reaching into each other's internals.

## Key Principles
- BIG focus on testing at all levels (unit, integration, API)
- Keep modules loosely coupled — each module owns its own entities, repositories, and services
- REST endpoints grouped by module (e.g. `/api/employees`, `/api/interactions`)
- Database schema separated per module where practical (schema-per-module or table prefix)
