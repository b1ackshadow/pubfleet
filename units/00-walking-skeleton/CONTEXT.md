# Unit 00 — walking skeleton

- **Tier:** T2
- **Status:** build
- **Branch:** `unit/00-walking-skeleton`
- **ADR:** docs/adr/001-walking-skeleton.md

## Purpose

Make one thin path work end to end. A supplier submits a job. A worker claims it, runs
trivial work, and reports a result. The console shows the state change.

Nothing here is clever. Its job is to give every later unit a place to plug in.

## Contract

**Modules.** `apps/control-plane`, `apps/worker`, `libs/contracts`.

**Rule for later units.** A `libs/` module is created when a unit publishes a port.
Never before. An empty module waiting to be filled is speculative structure.

**Job status.** `PENDING` to `CLAIMED` to `SUCCEEDED` or `FAILED`. No other transition
is legal.

**HTTP.** `POST /api/jobs`, `GET /api/jobs`, `GET /api/jobs/{id}`.

**Kafka.** Topic `pubfleet.job.completed`, keyed by job id.

**Ownership.** The control plane owns the job record and the terminal state. The worker
owns the claim only.

## Constraints given

- The path crosses a real process boundary. Two deployables, not two beans.
- `docker compose up -d` plus two commands produce a working system on a cold clone.
- Postgres holds job state. Kafka carries the work result. React shows it.
- No lease. No saga. No retry. No idempotency. Units 01 to 03 own those.

## Decisions

| # | Decision | Reason | Rejected option |
|---|---|---|---|
| 1 | Three modules: two apps, one contracts lib | Two deployables need a real serialization boundary | A `libs/` module per concern up front, rejected as speculative |
| 2 | Postgres owns work, Kafka carries results | Broker dispatch binds parallelism to partitions, has no per-message ack, and breaks on long jobs | Kafka as the work queue |
| 3 | `status` column with an explicit state machine | An outbox is a table of pending messages, not event sourcing. A status column supports it | Append-only event log with derived status |
| 4 | REST plus TanStack Query polling | Streaming has no consumer until unit 07 defines its shape | Server-sent events from the start |
| 5 | One integration test on Testcontainers, plus one Playwright test | A test with a mocked API would pass with the backend switched off | No browser test, verify the console by hand |

## Failure modes proven

| Failure mode | Test file | What it proves |
|---|---|---|
| Any hop breaks | `JobLifecycleIT` | pending |
| Console misses a state change | `job-lifecycle.spec.ts` | pending |

## Build summary

Not built.

## Open risks

**The claim races, on purpose.** `UPDATE ... WHERE id=? AND status='PENDING'` with no
`SKIP LOCKED`, no lease, and no expiry. Two workers can select the same row. A worker
that dies after claiming strands the job. This is unit 01's brief. Do not fix it here.

**The publish is not atomic with the write.** The control plane writes the row and then
publishes. A crash between the two loses the event. This is unit 03's outbox. Do not fix
it here.
