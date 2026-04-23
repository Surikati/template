# Service Subcharts

## Pattern

Each microservice is a thin subchart that imports common K8s resources from the **`tmpmgmt-service`** library chart. A service chart only carries what is unique to itself (port, env vars, resource sizing).

Layout per service:

```
charts/<service-name>/
├── Chart.yaml        # declares dependency on tmpmgmt-service
├── values.yaml       # port, env, resources, autoscaling
└── templates/
    └── all.yaml      # {{- include "tmpmgmt-service.deployment" . }} etc.
```

Shared templates live in `charts/tmpmgmt-service/templates/_*.tpl`:

- `_deployment.tpl` — Deployment with actuator probes, Prometheus scrape annotations
- `_service.tpl` — ClusterIP Service on the declared port
- `_hpa.tpl` — optional HorizontalPodAutoscaler
- `_helpers.tpl` — labels, selector labels, image reference builder

## Adding a new service chart

1. `cp -r charts/template-service charts/my-new-service` (or use `helm create` + trim)
2. Edit `Chart.yaml` (name, description), `values.yaml` (port, env, resources).
3. Append `- { name: my-new-service, version: 0.1.0, repository: "file://../charts/my-new-service" }` to the umbrella `template-management/Chart.yaml` dependencies.
4. `helm dependency update template-management/`.

## Reference chart

`template-service/` is the canonical reference. When changing the library chart, validate against `template-service` first.

## TODO

Currently only `template-service` has a full chart. Clone it for the remaining services:
`clause-service`, `questionnaire-service`, `assembly-service`, `rendering-service`, `document-service`, `admin-service`, `audit-service`, `search-service`, `api-gateway` (the last has a different ingress story — the gateway is the only component behind the Ingress).
