# Decision index

Two kinds of record live here. Design decisions point to an ADR. Plan decisions are
written in full, because they have no ADR.

An ADR is accepted only after the design survives the grill. The rejected option is
named in every ADR. A record without a rejected option is not finished.

---

## Design records

| ID | Title | Unit | Date | Status |
|---|---|---|---|---|
| — | none yet | — | — | — |

---

## Plan records

### PD-001 — The plan shape

**Date:** 2026-08-12

**Decision.** Build one platform. Each subsystem is a standalone library unit with
clean ports, designed first as an interview problem, then built and wired into the
running skeleton.

**Rejected.** Build each system standalone and integrate at the end. Rejected because
nothing runs end to end for months, and all the integration pain arrives at once.

### PD-002 — Division of labor

**Date:** 2026-08-12

**Decision.** Claude writes the code. The user owns every decision an interviewer
would probe, and defends it before the build starts. Learning is carried by three
mechanisms: decision points surfaced before the build, a walkthrough of the
non-obvious part after it, and a question bank that feeds later mock interviews.

**Rejected.** The user hand-writes the hard parts. Rejected on time cost. Machine
coding practice happens separately, not here.

### PD-003 — Unit ordering

**Date:** 2026-08-12

**Decision.** Product need picks the order. `SKILLS.md` proves the coverage. At each
milestone, an uncovered skill becomes a named constraint on an upcoming unit, a T3
design unit, or a recorded drop.

**Rejected.** Order by skill gap. Rejected because the product then bends to fit the
curriculum, and a system built as a museum of techniques does not survive push-back.

### PD-004 — Tiers

**Date:** 2026-08-12

**Decision.** Three tiers. T1 is a full build under interviewer stance. T2 is built and
wired under coach stance. T3 is design and grill only. The tier is set when the unit
enters the manifest. A tier rises only by a record in this file.

### PD-005 — Context management

**Date:** 2026-08-12

**Decision.** The main session is an orchestrator and does not read implementation
code. Subagents build, test, review, and document. Their summaries are written into
`units/<id>/CONTEXT.md` at the moment of the decision.

**Rejected.** Harness memory as the store. Rejected because in-repo files are
versioned, diffable, and readable by the user.

### PD-006 — Stack calls

**Date:** 2026-08-12

**Decision.** Maven over Gradle, because the target shops are Maven-heavy. Real Kafka
over Redpanda, because the vocabulary matters in the room. LocalStack only, with no
real AWS deployment. Gemini free tier behind a port, with a deterministic fake for
tests. Java 25, for the current LTS and for the fixed virtual-thread pinning behavior.

**Consequence.** Documents state LocalStack, never AWS. The Gemini quota forces real
backpressure in unit 07, which is useful rather than costly.

### PD-007 — Repository

**Date:** 2026-08-12

**Decision.** Public repository. One branch and one pull request per unit. CI must
pass before a merge. The README states the scope honestly, including the AI assistance
and the LocalStack limit.
