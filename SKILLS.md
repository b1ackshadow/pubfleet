# Coverage ledger

The curriculum. Product need picks the order of the units. This ledger proves the
coverage. At each milestone, read this file. Any skill still open becomes a named
constraint on an upcoming unit.

**Start level:** 🟢 proven, you did the thinking · 🟡 adjacent, a deliberate re-tool
· 🔴 gap, real study and reps · ⚪ edge, cover only if it is cheap.

**State:** `open` · `covered` (built) · `proven` (built, grilled, story written).

Sources: four staff-grade design docs of the real work, plus two target job
descriptions. A skill is here because a real system forced it, or because a hiring
manager asked for it.

**Rule:** if a skill cannot be justified inside this product, do not bolt it on.
Move it to a T3 design unit, or drop it and record why.

---

## 1. Core Java and the JVM

| Skill | Start | Units | State |
|---|---|---|---|
| Java 25 idioms: records, sealed types, pattern matching, `Optional` hygiene | 🟡 | 00 | open |
| Virtual threads: where they help, the pinning trap, why not for CPU work | 🔴 | 01, 04 | open |
| Structured concurrency: bounded fan-out, deadline cancellation | 🔴 | 04 | open |
| `ExecutorService`, `CompletableFuture`, atomics, `ConcurrentHashMap` | 🟡 | 01 | open |
| Thread-pool and connection-pool sizing math (HikariCP) | 🔴 | 14 | open |
| Bounded queues and backpressure; consumer pause and resume | 🟡 | 07 | open |
| Distributed locks and their failure modes: GC pause, clock skew, lease expiry | 🔴 | 01 | open |
| JVM in a container: `MaxRAMPercentage`, CPU throttling, G1 against ZGC | 🔴 | 13 | open |
| Java memory model: happens-before, `volatile`, safe publication | ⚪ | — | open |

## 2. Design, OOP, and DDD

| Skill | Start | Units | State |
|---|---|---|---|
| SOLID applied; composition over inheritance | 🟢 | all | open |
| DDD tactical: aggregates, value objects, invariants at the boundary | 🔴 | 03 | open |
| Ports and adapters; anti-corruption layer | 🔴 | 04, 07 | open |
| DTO to entity mapping; never leak an entity over the wire | 🟡 | 00 | open |
| Jackson depth: custom serializers, `BeanSerializerModifier`, annotations | 🟢 | 08 | open |
| Spring AOP for cross-cutting concerns: audit, encryption, authorization | 🟡 | 08 | open |
| Boundary rules enforced by ArchUnit | 🔴 | 00 | open |

## 3. Spring

| Skill | Start | Units | State |
|---|---|---|---|
| Spring Boot 3: auto-configuration, profiles, `@ConfigurationProperties` | 🟡 | 00 | open |
| `@Transactional` semantics and the self-invocation trap | 🟡 | 01 | open |
| Spring Data JPA: derived queries, projections, pagination | 🔴 | 01 | open |
| Spring Security: OAuth2 resource server, JWT, method authorization | 🟡 | 11 | open |
| Spring Kafka: listener concurrency, container pause and resume | 🟡 | 03 | open |
| Spring Boot starter authoring: `@AutoConfiguration`, property binding | 🟡 | 08 | open |
| Spring AI: `ChatClient`, streaming, provider swap | 🟡 | 07 | open |
| WebFlux trade-offs, and why virtual threads change the answer | 🔴 | 07 | open |
| Spring Batch for bulk transformation (JD 2) | 🔴 | 09 | open |

## 4. Data and storage

| Skill | Start | Units | State |
|---|---|---|---|
| Postgres indexes: B-tree, partial, composite; `EXPLAIN ANALYZE` | 🔴 | 09 | open |
| Transactions and isolation: read committed, repeatable read, serializable | 🔴 | 01 | open |
| Lost updates, phantom reads, non-repeatable reads | 🔴 | 01 | open |
| Locking: optimistic `@Version` against pessimistic `SELECT FOR UPDATE` | 🔴 | 01 | open |
| `SKIP LOCKED` as a queue primitive | 🔴 | 02 | open |
| Hibernate traps: N+1, `@EntityGraph`, lazy loading, `merge` against `persist` | 🔴 | 09 | open |
| Flyway migrations; expand and contract for zero downtime | 🔴 | 00, T4 | open |
| JSONB modeling with GIN indexes | 🔴 | 07 | open |
| Redis: structure choice, cache-aside, stampede defense, TTL and eviction | 🟡 | 09 | open |
| DynamoDB single-table design: partition key, sort key, GSI, TTL | 🟢 | 06 | open |
| OpenSearch: mapping, analyzers, relevance tuning (JD 2) | 🔴 | 09 | open |
| Access-pattern judgment: relational against NoSQL against search | 🟡 | 06, 09 | open |
| Capacity estimation: Little's Law, peak ratio, partition count | 🔴 | 14 | open |

## 5. Distributed systems and messaging

| Skill | Start | Units | State |
|---|---|---|---|
| CQRS read and write split | 🟢 | 09 | open |
| Saga orchestration against choreography; compensation order | 🟢 | 03 | open |
| Idempotency end to end: keys, dedup windows | 🟢 | 03 | open |
| Outbox and inbox: an atomic database write plus a publish | 🔴 | 03 | open |
| Kafka: partitions, keys, ordering, consumer groups, offsets, replication | 🟡 | 03 | open |
| Rebalancing: cooperative-sticky, static membership, rollout impact | 🔴 | 03 | open |
| Delivery semantics: exactly-once against at-least-once plus idempotent consumers | 🟡 | 03 | open |
| Schema registry and evolution; dead-letter queues; poison pills; replay | 🟡 | 03 | open |
| Bounded-context decomposition; when not to split | 🟡 | 00 | open |
| Event-driven design on EventBridge, SQS, and SNS (JD 2) | 🟡 | 06 | open |

## 6. Resilience

| Skill | Start | Units | State |
|---|---|---|---|
| Resilience4j: circuit breaker, retry with jitter, timeout, bulkhead | 🟡 | 04 | open |
| Decorator ordering, and why the order changes the behavior | 🟡 | 04 | open |
| One breaker and one bulkhead per external dependency | 🟡 | 04 | open |
| Graceful degradation, fallbacks, partial results | 🟢 | 04 | open |
| Failure-mode enumeration and game days | 🟡 | 12 | open |

## 7. API design

| Skill | Start | Units | State |
|---|---|---|---|
| OpenAPI as the contract, with springdoc | 🔴 | 00 | open |
| RFC 9457 problem-detail error contract | 🔴 | 00 | open |
| Versioning policy and a deprecation path | 🔴 | 04 | open |
| Cursor and keyset pagination, and why offset does not scale | 🔴 | 09 | open |
| REST verbs and status codes | 🟢 | 00 | open |
| Server-sent events and WebSocket streaming | 🟡 | 07, 11 | open |
| Idempotency keys on mutating endpoints | 🟢 | 04 | open |

## 8. Cloud, serverless, and infrastructure

| Skill | Start | Units | State |
|---|---|---|---|
| Kubernetes: deployments, probes, resource limits, HPA and KEDA | 🔴 | 13 | open |
| PodDisruptionBudget, graceful drain, rollout and canary | 🔴 | 13 | open |
| Docker multi-stage builds; small layered images | 🟡 | 00 | open |
| AWS Lambda, API Gateway, Step Functions (JD 1 and JD 2) | 🟡 | 06 | open |
| AWS CDK in TypeScript as infrastructure as code | 🟡 | 06 | open |
| LocalStack as the AWS test environment | 🟡 | 06 | open |
| GitHub Actions pipeline: build, test, scan, deploy | 🟡 | 00 | open |
| Secrets handling and key rotation | 🟡 | 08 | open |
| Multi-region active-active; RPO and RTO | 🔴 | T1 | open |

## 9. Security and cryptography

| Skill | Start | Units | State |
|---|---|---|---|
| OAuth2, JWT, method-level authorization | 🟡 | 11 | open |
| Multi-tenant isolation: IDOR, Postgres row-level security | 🟡 | T3 | open |
| Envelope encryption with KMS; per-context data keys; key caching | 🔴 | 08 | open |
| AES-256-GCM authenticated encryption | 🔴 | 08 | open |
| Key rotation with a version inside the ciphertext | 🔴 | 08 | open |
| Blind index with a keyed HMAC for searchable encrypted fields | ⚪ | 08 | open |
| LLM prompt injection; PII redaction before a model call | 🟡 | 07 | open |
| Log scrubbing and allowlists | 🟡 | 12 | open |

## 10. Observability

| Skill | Start | Units | State |
|---|---|---|---|
| Micrometer to Prometheus to Grafana | 🔴 | 12 | open |
| The domain metrics that matter: compensations, contention, lag, hit rate | 🔴 | 12 | open |
| OpenTelemetry tracing across services and through Kafka headers | 🔴 | 12 | open |
| Structured JSON logs with a run id in the MDC | 🔴 | 12 | open |
| SLIs, SLOs, and burn-rate alerts | 🔴 | 12 | open |

## 11. Testing and quality

| Skill | Start | Units | State |
|---|---|---|---|
| JUnit 5, Mockito, and slice tests | 🟡 | 00 | open |
| Testcontainers with real Postgres, Kafka, and Redis | 🔴 | 01 | open |
| The race test that passes on H2 and lies | 🔴 | 01 | open |
| WireMock for a flaky external partner | 🟡 | 04 | open |
| Playwright end-to-end tests | 🟡 | 11 | open |
| Vitest and React Testing Library (JD 2) | 🟡 | 11 | open |
| Load testing with k6, to validate the capacity numbers | 🟡 | 14 | open |
| Property-based and mutation testing | ⚪ | — | open |

## 12. Front end

| Skill | Start | Units | State |
|---|---|---|---|
| React 19 hooks, composition, and effects done right | 🟢 | 11 | open |
| TypeScript: generics, discriminated unions, no `any` | 🟢 | 11 | open |
| TanStack Query for server state | 🟡 | 11 | open |
| MUI component system and theming (JD 2) | 🟡 | 11 | open |
| React Router and route guards (JD 2) | 🟡 | 11 | open |
| Token handling and auth on the client | 🟡 | 11 | open |
| Streaming into the UI over server-sent events | 🟡 | 07, 11 | open |
| Code splitting, memoization, and Suspense | 🟡 | 11 | open |

## 13. Judgment and seniority signals

| Skill | Start | Units | State |
|---|---|---|---|
| Capacity to threading to autoscaling to cost, as one story | 🔴 | 14 | open |
| ADR discipline: decide, defend, name the rejected option | 🟡 | all | open |
| Trade-off articulation out loud, under push-back | 🟡 | all T1 | open |
| Knowing when not to build | 🟢 | all | open |
| Branching strategy and pull-request workflow (JD 2) | 🟡 | all | open |
| Written design documents for a mixed audience (JD 2) | 🟡 | all | open |
| AI-assisted development: validate, refine, and defend the output (JD 1) | 🟡 | all | open |
| Responsible AI use: data sensitivity, safe input and output handling (JD 1) | 🟡 | 07 | open |

---

## Milestone check

Read this file after units 04, 09, and 14. For each open 🔴 skill, do one of three
things. Attach it as a named constraint on an upcoming unit. Move it to a T3 design
unit. Drop it, and record the reason in `DECISIONS.md`.
