# Interview stories

One story per unit, in STAR form. Written at the end of the unit, while the detail is
fresh. A story without a number and without a rejected option is not finished.

Each story names the real role that produced the concept. See `SEEDS.md`.

**Template:**

### S-NN — <title>

- **Concept from:** <role>
- **Unit:** <id>
- **Situation:** the constraint, in two sentences.
- **Task:** what had to be true.
- **Action:** the decision, and the option that was rejected.
- **Result:** the number, and the conditions that produced it.
- **The push-back:** the hardest question asked during the grill, and the answer.

---

### S-00 — The broker was the obvious answer and the wrong one

- **Concept from:** Tesco
- **Unit:** 00
- **Situation:** A control plane had to dispatch work to a fleet that grows from 10 to
  about 200 workers. Kafka was already in the stack, so putting the jobs on a topic was
  the default choice.
- **Task:** One worker runs one job. A dead worker must not strand it. A failed job must
  be retried without touching its neighbors.
- **Action:** I put ownership in Postgres and left Kafka to carry results. A consumer
  group binds parallelism to the partition count, so 200 workers force 200 partitions.
  Offsets commit ranges, so there is no per-message acknowledgement. A job that runs for
  minutes exceeds `max.poll.interval.ms`, and the broker moves the partition while the
  work is still running, which duplicates execution. I rejected broker dispatch for those
  reasons, and I keep the condition that would reverse it: short uniform tasks that need
  no individual redelivery, at a volume Postgres cannot take.
- **Result:** Both legs are load-bearing, so the end-to-end test fails if either Postgres
  or Kafka breaks. Proven by pointing the consumer at a wrong topic and watching both
  test methods fail.
- **The push-back:** "Why two deployables when one process is simpler?" In one process
  the claim is a method call, so a worker cannot die independently. An orphaned lease
  cannot be observed, a misfire cannot happen, and the outbox has no motivation.
