# Template Management

Enterprise on-premise template management system — tvorba, správa a generování dokumentů ze šablon s podporou proměnných, podmínek, opakovačů a knihovny doložek.

## Architektura

- **Backend:** Java 21, Spring Boot 3.3, mikroslužby, Maven multi-module
- **Frontend:** Angular 17+, TipTap WYSIWYG, PrimeNG
- **Data:** PostgreSQL 16 (DB per service), MinIO (object storage), OpenSearch (fulltext)
- **Messaging:** RabbitMQ + transactional outbox
- **Auth:** Keycloak (LDAP/AD federation, OIDC)
- **Deploy:** Vanilla Kubernetes + Helm

Viz [`docs/architecture.md`](docs/architecture.md) a [`docs/domain-model.md`](docs/domain-model.md).

## Struktura monorepa

```
backend/         Maven parent + shared libs + mikroslužby
frontend/        Angular workspace (shell + libs)
infra/           docker-compose pro lokální dev, Helm charts, Keycloak realm
docs/            architektura, doménový model, návody
.github/         GitHub Actions CI
```

## Rychlý start (lokální dev)

```bash
# 1. Zvedni závislosti (Postgres, RabbitMQ, Keycloak, MinIO, OpenSearch)
cd infra/docker-compose
docker compose up -d

# 2. Postav backend
cd ../../backend
./mvnw clean install

# 3. Spusť jednotlivou službu
cd services/template-service
../../mvnw spring-boot:run

# 4. Postav a spusť frontend
cd ../../../frontend
npm install
npm start
```

Detailní návod: [`docs/getting-started.md`](docs/getting-started.md).

## Mikroslužby (MVP)

| Služba | Port | Odpovědnost |
|---|---|---|
| `api-gateway` | 8080 | Routing, token validation, rate limiting |
| `template-service` | 8081 | CRUD šablon, verzování, draft/publish |
| `clause-service` | 8082 | Knihovna doložek, verzování |
| `questionnaire-service` | 8083 | Definice a běh interaktivních průvodců |
| `assembly-service` | 8084 | Orchestrace generování dokumentu |
| `rendering-service` | 8085 | Stateless DOCX/PDF/HTML rendering |
| `document-service` | 8086 | Úložiště vygenerovaných dokumentů |
| `admin-service` | 8087 | Uživatelé, role, konfigurace |
| `audit-service` | 8088 | Append-only audit log |
| `search-service` | 8089 | Fulltext vyhledávání (OpenSearch) |

## Licence

TBD.
