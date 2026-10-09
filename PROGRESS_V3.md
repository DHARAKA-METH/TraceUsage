# TraceUsage V3 — Implementation Progress

API lifecycle and deprecation management.

Status legend: `[ ]` pending · `[~]` in progress · `[x]` done

---

## Phase 1 — Database and configuration

- [x] Step 1 — Application monitoring settings (`monitoring_started_at`, `inactivity_threshold_days`)
      → `V8__add_api_lifecycle.sql`, `Application` entity
- [x] Step 2 — API endpoint registry (`api_endpoints`) + backfill from `usage_events`
      → `ApiEndpoint`, `ApiEndpointRepository`, `EndpointRegistryService`
- [x] Step 3 — Endpoint deprecation table (`endpoint_deprecations`)
      → `EndpointDeprecation`, `EndpointDeprecationRepository`

## Phase 2 — Lifecycle evaluation logic

- [x] Step 4 — `LifecycleStatus` enum
- [x] Step 5 — `LifecycleEvaluationInput` record
- [x] Step 6 — `LifecycleStatusEvaluator` component
- [x] Step 7 — Worked examples covered by 14 unit tests

## Phase 3 — Services and APIs

- [x] Step 8 — `EndpointLifecycleService` (last seen from V1 telemetry)
- [x] Step 9 — Lifecycle GET APIs
      → `GET /api/v1/applications/{projectId}/endpoints/lifecycle`
      → `GET /api/v1/applications/{projectId}/endpoints/{endpointId}/lifecycle`
- [x] Step 10 — Deprecation POST API
      → `POST /api/v1/applications/{projectId}/endpoints/{endpointId}/deprecations`

## Phase 4 — Known-client analysis and removal rule

- [x] Step 11 — Known-client analysis via V2 `field_usage_events`
      → `KnownClientAnalyzer`
- [x] Step 12 — Removal-candidate rule with coverage gates

## Phase 5 — Dashboard and testing

- [x] Step 13 — Next.js lifecycle page + deprecation form
      → `/applications/[projectId]/lifecycle`
- [x] Step 14 — Unit tests (18 lifecycle tests, 59 total, all green)
- [x] Step 15 — End-to-end flow documented below

---

## Design decisions

1. **Status is computed on read**, never stored. Prevents stale lifecycle state.
2. **URLs use `projectId`**, not numeric `applicationId`, to match existing
   `/api/v1/applications/{projectId}/analytics/*` endpoints. Ownership is still
   enforced with the authenticated user id.
3. **No automatic deletion.** V3 only produces evidence for manual review.
4. **Client coverage is derived from V2 telemetry.** V1 events carry no client
   identity, so `clientCoverageSufficient` is false unless at least one
   `field_usage_events` row exists for that endpoint. This stops V3 from
   claiming "no active clients" when client data was never collected.
5. **Wording is evidence-based**: `REMOVAL_CANDIDATE` means "eligible for manual
   review", never "safe to delete".
6. **Endpoint registry self-populates.** `EndpointRegistryService` is called from
   `TelemetryServiceImpl` on every V1 event, so new endpoints are registered
   automatically without manual imports.

---

## How the removal rule behaves in practice

Because `clientCoverageSufficient` requires V2 field telemetry for the endpoint,
a V1-only application will stay at `DEPRECATED_INACTIVE`. That is intentional.

To reach `REMOVAL_CANDIDATE` you need all of:

- monitoring history longer than the inactivity threshold
- deprecated for at least the threshold duration
- no requests inside the threshold window
- at least one `field_usage_events` row for that endpoint (client coverage)
- zero distinct active clients in that window

---

## End-to-end verification steps

1. Start backend: `cd TraceUsage && sh mvnw spring-boot:run`
2. Start demo API: `sh TraceUsage/mvnw spring-boot:run -pl traceusage-demo-api`
3. Call `GET http://localhost:8081/api/products/10` so V1 telemetry arrives
4. Start dashboard: `cd traceusage-dashboard && npm run dev`
5. Open `http://localhost:3000`, login, go to the application
6. Open the **Lifecycle** tab
7. Expect `NEWLY_MONITORED` for a brand new application
8. Mark the endpoint deprecated
9. Expect `DEPRECATED_ACTIVE` while requests continue

Migration `V8` is applied automatically by Flyway on startup.

---

## Resume notes

All 15 steps are implemented and verified by the test suite. If work resumes,
start from the end-to-end verification above or extend V3 with:

- scheduled aggregation of `api_endpoints` counters instead of per-event updates
- Spring `RequestMappingHandlerMapping` discovery so never-called endpoints appear
- configurable removal thresholds per environment
- API-key scoped client identity for V1 events to improve client coverage