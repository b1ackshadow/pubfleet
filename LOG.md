# Session log

One entry per session. A session ends with its entry written. If the entry is not
written, the session did not happen.

**Template:**

### YYYY-MM-DD — unit NN — mode

- **Did:** one line.
- **Decided:** the decisions made, and where they are recorded.
- **Missed:** what the user could not answer or defend.
- **Next:** the single next action.

---

### 2026-08-12 — setup — plan

- **Did:** created the repository, the operating contract, the manifest, the coverage
  ledger, the concept seeds, and the decision index.
- **Decided:** PD-001 through PD-007 in `DECISIONS.md`.
- **Missed:** nothing yet.
- **Next:** open unit 00 in `design` mode.

### 2026-08-13 — unit 00 — design, build, walkthrough

- **Did:** designed and built the walking skeleton. Two deployables, Postgres owns the
  claim, Kafka carries the result. Console, tests, toolchain, and CI. PR #1 open, CI green,
  including `JobLifecycleIT` on real Testcontainers.
- **Decided:** ADR-001. Five decisions in `units/00-walking-skeleton/CONTEXT.md`.
- **Missed:** the user did not question the two-deployable choice before accepting it.
  That is the point an interviewer presses first. Answer is now in the walkthrough.
- **Review:** 10 findings, all real. Six fixed, three deferred with a named closing unit,
  one handed to unit 01. The worst was a try block wide enough to report FAILED for work
  that had succeeded.
- **Next:** merge PR #1, then open unit 01 in `design` mode with interviewer stance.
