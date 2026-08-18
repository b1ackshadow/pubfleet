# ADR-001 — Walking skeleton: two deployables, Postgres owns work, Kafka carries results

- **Unit:** 00
- **Date:** 2026-08-13
- **Status:** accepted

## Context

One thin path must work end to end before any other unit starts. A supplier submits a
job. A worker runs it. An operator sees the state change. Later units add a lease,
a scheduler, a saga, and partner adapters. Each one needs a place to plug in.

The invariant for this unit is small: a submitted job reaches a terminal state, and
every hop in the chain is real.

## Options

| Option | Buys | Costs |
|---|---|---|
| One process, module boundaries only | Fast, simple, no network | Hides every problem units 01 to 04 exist to teach |
| Two deployables, Kafka delivers work | Broker does the queueing | Parallelism tied to partition count; no per-message ack; rebalance storms on long jobs |
| Two deployables, Postgres owns work, Kafka carries results | Ownership is transactional; both legs load-bearing | Two systems to run; a naive claim has a race |

## Decision

Two deployables. The control plane owns the job record and terminal state. The worker
polls Postgres and claims a job. The worker publishes the result to Kafka. The control
plane consumes that result and writes the terminal state.

Postgres holds who owns what work right now. Kafka carries commands and events. Both
legs are load-bearing, so the end-to-end test fails if either breaks.

## Rejected

**Kafka as the work queue.** Four properties make it wrong for job dispatch. Consumer
parallelism is bound to the partition count, so a 200-worker fleet needs 200 partitions.
Offsets commit ranges, so there is no per-message acknowledgement. A job that runs for
minutes exceeds `max.poll.interval.ms` and the broker moves the partition while the work
continues. There is no visibility timeout and no redelivery to a different worker.

Kafka becomes the right answer for dispatch when jobs are short, uniform, and need no
individual redelivery.

**One process with Spring Modulith.** The correct choice for a real product that must
avoid the distributed-monolith trap. Wrong here, because the distributed problems are
the curriculum, not a cost to defer.

## Consequences

Easier: units 01 to 04 have a real seam to open. The contract module becomes a genuine
serialization boundary, so schema evolution is a real problem later.

Harder: two processes to run and to trace. Local start-up needs Docker Compose.

Monitored: the claim is naive on purpose. See the failure modes below.

## Known defect, left in on purpose

The claim is `UPDATE jobs SET status='CLAIMED', worker_id=? WHERE id=? AND status='PENDING'`.
There is no `SKIP LOCKED`, no lease, and no expiry. Two workers that poll at the same time
select the same row, and a worker that dies after a claim strands the job forever.

This is unit 01's brief. Unit 01 opens with a test that proves the race, then replaces
the claim. Do not fix it here.

## Failure modes proven

| Failure mode | Test | Result |
|---|---|---|
| Any hop in the chain breaks | `JobLifecycleIT` test 1, on Testcontainers | passes; proven to fail when the consumer topic is wrong |
| A terminal job is overwritten | `JobLifecycleIT` test 2 | passes; fenced by a second job on the same partition |
| The console does not reflect a state change | `job-lifecycle.spec.ts` | passes, 10 runs of 10, against the live stack |

## The push-back

**Asked:** why two deployables when one JVM is simpler?
**Answered:** a single process makes the claim a method call. A worker can then never
die independently, so an orphaned lease cannot be observed, a misfire cannot happen, and
the outbox has no motivation. The units that follow would each lose the reason they exist.

## The contract

Job status: `PENDING` to `CLAIMED` to `SUCCEEDED` or `FAILED`. Transitions are explicit
and no other transition is legal.

| Endpoint | Purpose |
|---|---|
| `POST /api/jobs` | Submit. Returns 202 and the job id. |
| `GET /api/jobs` | List, with keyset pagination. |
| `GET /api/jobs/{id}` | Read one. |

| Topic | Key | Payload |
|---|---|---|
| `pubfleet.job.completed` | job id | job id, worker id, outcome, result, finished at |
