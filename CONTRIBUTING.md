# Contributing

## Větvení

- `main` — stabilní, produkční. Do `main` jen přes PR.
- `develop` — integrační větev.
- `feature/<jira-id>-<slug>` — nová funkcionalita.
- `bugfix/<jira-id>-<slug>` — oprava chyby.
- `hotfix/<slug>` — kritická oprava na `main`.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

Typy: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`.

Scope = jméno služby (`template-service`, `frontend`, `infra`) nebo shared modulu.

Příklad:
```
feat(template-service): přidat endpoint pro publish draft verze

- Implementuje POST /api/v1/templates/{id}/versions
- Validuje variablesSchema proti JSON Schema Draft 2020-12
- Emituje template.version.published event přes outbox

Refs: TMPMGMT-42
```

## Pull requesty

1. Aktualizuj větev proti `develop` (rebase preferovaný před merge commitem).
2. Zelené CI (build + testy + lint).
3. Minimálně 1 code review approve.
4. Squash merge (pokud PR = jedna logická změna), jinak merge commit.

## Kódová konvence

**Backend (Java):**
- Java 21, konvence Google Java Style Guide.
- Lombok povolen, ale střídmě (`@Value`, `@Builder`, `@RequiredArgsConstructor`).
- Testy: JUnit 5 + AssertJ + Testcontainers pro integrační.

**Frontend (Angular):**
- Angular 17+, standalone components, signals preferovány před RxJS pro local state.
- Striktní TypeScript (`"strict": true`).
- Testy: Jest (unit), Playwright (E2E).

## Code review — na co se dívat

- Dodržení [designových rozhodnutí](docs/architecture.md) — immutable versions, outbox pattern, restricted expressions.
- Bezpečnost — žádné SpEL/MVEL s uživatelským vstupem, SQL injection, XSS.
- Veřejné API — pokud se mění, update i OpenAPI spec.
