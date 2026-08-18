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
| Any hop breaks | `JobLifecycleIT` test 1 | Submit, claim, publish, consume, terminal state, on real Postgres and real Kafka. The consumer topic was pointed at a wrong name and both methods failed, then reverted. |
| A terminal job is overwritten | `JobLifecycleIT` test 2 | A completion event for an already-terminal job changes nothing. A second job on the same partition fences the assertion, so the illegal event is proven to have been consumed and rejected. |
| Console misses a state change | `job-lifecycle.spec.ts` | The rendered status walks forward to `SUCCEEDED`. Passed 10 runs out of 10 against the live stack. |

The integration test copies the two SQL statements of the worker rather than depending on
the worker module, because that would put two deployables on one classpath. The test reads
`JobClaimRepository.java` and fails if the copy has drifted.

## Build summary

**Console** — `apps/console/`. React 19, Vite, MUI v6, TanStack Query v5, one route.
`JobStatus` is a closed literal union with a type guard. The status colour map is an
exhaustive `Record<JobStatus, ...>`, so a new status breaks the build. Polling stops when
the tab is hidden, through TanStack Query's `refetchIntervalInBackground: false`. A
hand-written `usePageVisible` hook did the same job at first. The review proved the
library already gated the poll on `focusManager.isFocused()`, so the hook was deleted.
12 tests in 3 files. `npm run build`, `lint`, and `test` all pass.

**Backend** — Maven reactor, Spring Boot 3.5.16, `release=25`, failsafe wired for `*IT`.

- `libs/contracts` — status enum, request and response records, `JobCompletedEvent`. Jackson annotations only, no Spring class.
- `apps/control-plane` — controller on the frozen API, `JobStateMachine` as the single transition guard, RFC 9457 error handler, keyset queries on `(created_at, id)` descending, Kafka consumer, Flyway `V1__jobs.sql`, virtual threads on.
- `apps/worker` — plain JDBC, not JPA, so the claim is literally the mandated statement. Fixed-delay poller, trivial runner, Kafka producer. No web server.
- `docker-compose.yml` — Postgres 17 and Kafka in KRaft mode, both with healthchecks. No app containers.

27 tests pass on `./mvnw clean verify`. Both ArchUnit rules were verified to fail when
deliberately broken, then reverted. The full stack was booted and the whole path walked:
submit, claim, publish, consume, terminal state. Keyset paging walked 6 jobs with no gaps
and no repeats.

**Toolchain and CI** — `flake.nix` gives a dev shell with JDK 25, Maven, Node 24, and
the Playwright browsers, pinned to `nixos-26.05`. `nix develop` is the one command that
makes a cold clone build. CI runs three jobs: the Maven build with its Testcontainers
tests, the console lint, build, and unit tests, and a secret scan that fails the build on
a finding. Playwright is not in CI. It needs the live stack and runs locally for now.

**Port** — the control plane listens on 8081 by default, through
`PUBFLEET_CONTROL_PLANE_PORT`. Port 8080 is held by an unrelated service on this machine.
The Vite proxy targets 8081.

## Open risks

**The claim races, on purpose.** `UPDATE ... WHERE id=? AND status='PENDING'` with no
`SKIP LOCKED`, no lease, and no expiry. Two workers can select the same row. A worker
that dies after claiming strands the job. This is unit 01's brief. Do not fix it here.

**The publish is not atomic with the write.** The control plane writes the row and then
publishes. A crash between the two loses the event. This is unit 03's outbox. Do not fix
it here.

**A completing worker is not checked against the claim holder.** Any worker id can
complete a `CLAIMED` job and rewrite the attribution. The integration test proves the
terminal guard, not ownership. This is unit 01's fencing. Do not fix it here.

## Deferred from the unit 00 review

Real findings, recorded rather than fixed, with the unit that will close each one.

| Finding | Why deferred | Closes in |
|---|---|---|
| `GET /api/jobs` keyset paging has no test. The row-comparison query and the unknown-cursor behavior are asserted only in a comment. | Paging carries no weight until the catalog is large. The manual walk of 6 jobs was a check, not a guard. | 09 |
| A production console build points at an absolute `http://localhost:8081`, and the control plane sets no CORS policy. `npm run preview` therefore renders a console whose calls are all blocked. | Only the dev flow is used today. The console is rebuilt properly in unit 11. | 11 |
| The Playwright spec accepts `PENDING` or `CLAIMED` as the first rendered status, so a console that stopped polling could still pass. | Strict `PENDING` flakes 2 runs in 13. The poll interval is a unit 11 concern. | 11 |
