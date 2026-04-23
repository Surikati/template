# Local Development Stack

Spins up all infrastructure dependencies for running Template Management services locally.

## Prerequisites

- Docker Desktop / OrbStack / Rancher Desktop
- ~6 GB free RAM (OpenSearch + Keycloak + Postgres)

## Start

```bash
docker compose up -d
```

First startup takes a minute while Postgres initializes per-service databases, Keycloak imports the realm, and MinIO creates buckets.

## What's running

| Component | Port | UI/Endpoint | Credentials |
|---|---|---|---|
| PostgreSQL | 5432 | — | `postgres` / `postgres` |
| RabbitMQ | 5672 / 15672 | http://localhost:15672 | `guest` / `guest` |
| Keycloak | 8180 | http://localhost:8180 | `admin` / `admin` |
| MinIO | 9000 / 9001 | http://localhost:9001 | `minioadmin` / `minioadmin` |
| OpenSearch | 9200 | http://localhost:9200 | — |
| Prometheus | 9090 | http://localhost:9090 | — |
| Grafana | 3000 | http://localhost:3000 | `admin` / `admin` |

## Seeded Keycloak users

- `admin.user` / `admin` → role `ADMIN`
- `editor.user` / `editor` → roles `TEMPLATE_EDITOR`, `CLAUSE_EDITOR`, `USER`

## Tear down

```bash
docker compose down              # keeps volumes
docker compose down -v           # wipes data (fresh start)
```
