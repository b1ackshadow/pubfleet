# Concept seeds from real work

Four roles supply the concepts this platform exercises. This file holds the idea of
each one, not the implementation. It exists so that a unit brief can point at the
real problem that produced the concept.

Read at most one section per unit. Do not treat any section as a blueprint.

---

## Tesco — orchestration of a distributed worker fleet

**The real problem.** Automation jobs ran from cron on separate boxes. No central
view existed. Nobody could answer "did the nightly job run?" without a shell login.
The fleet had to grow from 10 workers to about 200.

**Why it is hard.** Correctness is the product, not throughput. A job that runs twice
double-posts to a ledger. A job that dies part way leaves the target system torn.
The platform must make a run exactly-once at the business level, and compensatable
when a chain of steps fails.

**Concepts it forces.** Central control plane. Worker registry and reservation.
Lease with an expiry. Saga with reverse-order compensation. Transactional outbox and
inbox. Idempotency keys across three trigger types. Per-run ordering by partition key.
A reusable starter and a CLI, so a new workload takes a day.

**Units:** 01, 02, 03.

---

## Mumfy — real-time ad matching at 200K users

**The real problem.** Serve a personalized ad in single-digit milliseconds at p99,
against about 20K active campaigns. A head of large advertisers targets the whole
user base. That head is a hot-key risk.

**Why it is hard.** The read path and the write path want opposite things. Scoring at
request time is too slow. So the system splits: a stream job scores and matches at
publish time, and writes a materialized candidate set. The serve path does one cache
round trip. The cost of that split is staleness.

**Concepts it forces.** CQRS. Precompute at publish. Stateful stream processing with
windowed joins. Cache-aside with TTL, and the stampede problem. Hot keys. Partition
key choice that keeps per-entity ordering while the consumer group scales out.
Budget pacing and depletion. Frequency capping. Freshness as one number that the TTL,
the SLO, and the consumer-lag budget all express.

**Units:** 09, 10.

---

## Rootent — AI extraction in a real-estate marketplace

**The real problem.** An agent typed more than 30 fields per listing by hand. That
killed throughput. The fix: drop a PDF brochure or a scanned document, and get a draft
listing in one step. A text and voice assistant then drives a live panel in the browser.

**Why it is hard.** The pipeline is asynchronous, failure-prone, and it costs money per
call. The model returns text that must become a typed record. Public listings must also
rank and paint fast, which forces a real rendering strategy.

**Concepts it forces.** Object storage, queue, and worker. Schema-constrained model
output, with a validation and repair loop. A cost and token budget. Backpressure
against a provider quota. PII redaction before the call. Streaming tokens to the
browser over server-sent events. A human review queue. Evaluation of extraction
accuracy. Role-based access for agent and admin.

**Units:** 07, 11.

---

## Amazon — carrier integration and a shared PII library

**The real problem, part A.** Sellers fulfill orders into new countries. The company
owns no trucks. It integrates about 40 third-party carriers, each one heterogeneous,
flaky, and rate-limited. One shipment quote fans out across many carriers at once.

**The real problem, part B.** Many services handle sensitive fields. Each one solved
encryption differently. A shared library encrypts marked fields at the serialization
boundary, driven by an annotation.

**Why it is hard.** A slow carrier must not consume the thread budget of the others.
A carrier that returns a bad response must not corrupt the internal model. The
encryption library must add near-zero latency to a hot path, and must survive a key
rotation without a rewrite of stored data.

**Concepts it forces.** An adapter per partner behind an anti-corruption layer.
Circuit breaker, retry with backoff and jitter, timeout, and bulkhead, in the correct
decorator order. A per-partner rate budget. Bounded fan-out with a deadline.
Envelope encryption with per-context data keys, a data-key cache, and a key version
inside the ciphertext. Annotation-driven field detection in Jackson.

**Units:** 04, 05, 06, 08.
