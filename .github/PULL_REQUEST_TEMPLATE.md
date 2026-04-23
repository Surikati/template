## Summary

<!-- What changes, and why. -->

## Touched service(s)

- [ ] api-gateway
- [ ] template-service
- [ ] clause-service
- [ ] questionnaire-service
- [ ] assembly-service
- [ ] rendering-service
- [ ] document-service
- [ ] admin-service
- [ ] audit-service
- [ ] search-service
- [ ] frontend
- [ ] infra (helm / docker-compose)

## Test plan

- [ ] `./mvnw verify` passes
- [ ] Unit tests added for new logic
- [ ] Integration tests (Testcontainers) where boundaries changed
- [ ] For FE: manually verified in browser

## Related

- Jira: TMPMGMT-
- Design doc: <!-- link if applicable -->

## Checklist

- [ ] DB migrations added (if schema changed)
- [ ] OpenAPI spec updated (if API changed)
- [ ] Outbox events added for new state transitions
- [ ] Follows [design decisions](docs/architecture.md) (immutable versions, restricted expressions, etc.)
