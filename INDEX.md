# Unit manifest

The single source of truth for position. Read this first in every session.

**Status values:** `todo`, `design`, `build`, `done`, `parked`.

**Tiers:** T1 = full build with an interviewer-stance grill. T2 = built and wired,
coach stance. T3 = design and grill only, no production code.

---

## Build units

| # | Unit | Tier | Status | Core concepts | Depends on |
|---|---|---|---|---|---|
| 00 | walking-skeleton | T2 | todo | trigger to worker to console; Postgres, Kafka, React shell | — |
| 01 | worker-lease | T1 | todo | `SELECT FOR UPDATE` against `@Version` against a Redis lease; isolation levels; race proof | 00 |
| 02 | scheduler | T1 | todo | database polling against a time wheel against Quartz; misfire; catch-up; DST | 01 |
| 03 | saga-outbox | T1 | todo | compensation order; outbox and inbox; effectively-once; idempotency keys | 01, 02 |
| 04 | partner-ingress | T1 | todo | circuit breaker; retry with jitter; bulkhead; anti-corruption layer; WireMock contracts | 03 |
| 05 | rate-limiter | T2 | todo | token bucket against sliding window; distributed counters in Redis; per-partner budgets | 04 |
| 06 | serverless-edge | T2 | todo | CDK; API Gateway; Lambda; DynamoDB single-table; EventBridge; Step Functions | 04 |
| 07 | ai-extraction | T1 | todo | schema-constrained output; repair loop; backpressure against a quota; PII redaction; SSE | 03, 05 |
| 08 | field-encryption | T2 | todo | Jackson serializers; annotation-driven detection; envelope encryption; key rotation | 07 |
| 09 | catalog-search | T1 | todo | OpenSearch; candidate to filter to score; cache-aside; hot keys | 07 |
| 10 | promotions | T2 | todo | budget pacing; frequency capping; high-rate counters | 09 |
| 11 | operator-console | T1 | todo | React; OAuth2 and JWT; RBAC; live status; streaming; Playwright | 09 |
| 12 | observability | T2 | todo | OpenTelemetry; RED metrics; one trace across the saga; SLOs | 11 |
| 13 | kubernetes | T2 | todo | k3d; probes; JVM in a container; HPA and KEDA | 12 |
| 14 | capacity | T2 | todo | k6 load test; Little's Law; real p99 numbers for the stories | 13 |

## Design-only tail

| # | Unit | Tier | Status | Core concepts |
|---|---|---|---|---|
| T1 | multi-region | T3 | todo | active-active; global tables; RPO and RTO; data locality |
| T2 | sharding | T3 | todo | partition keys; resharding; the celebrity problem |
| T3 | tenant-isolation | T3 | todo | row-level security; IDOR; noisy neighbors |
| T4 | zero-downtime-migration | T3 | todo | expand and contract; backfill; dual write |

---

## Ports published by units

A port is a reusable interface. Later units consume ports. They do not consume internals.

| Port | Owner unit | State |
|---|---|---|
| — | — | none yet |

---

## Position

- Current unit: **00 walking-skeleton**
- Last session: none
- Units done: 0 of 15 build units, 0 of 4 design units
