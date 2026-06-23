# Docker & Deployment

## Local Development Stack
Use Docker Compose to run:
- PostgreSQL container (port 5432)
- Backend Spring Boot app (port 8080)
- Frontend Angular dev server (port 4200)

## Docker Compose Structure
```
Staff-Engagement-POC/
├── docker-compose.yml
├── backend/
│   └── Dockerfile
└── frontend/
    └── Dockerfile
```

## PostgreSQL Container
- Image: `postgres:16`
- Default dev credentials configured via environment variables
- Volume for data persistence in local dev
- Init scripts for schema creation if needed

## Backend Dockerfile
- Multi-stage build: Maven build → JRE 21 runtime
- Expose port 8080
- Spring profile `docker` for container-specific config (datasource URL points to compose service name)

## Frontend Dockerfile
- Multi-stage build: Node build → Nginx for serving static files
- Expose port 80 (production) or use `ng serve` in dev mode

## Environment Configuration
- Backend uses Spring profiles (`local`, `docker`, `test`)
- Database connection configured via environment variables (not hardcoded)
- Frontend API URL configurable per environment
