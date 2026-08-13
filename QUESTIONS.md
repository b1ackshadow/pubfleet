# Question bank

Every drill question lands here. `mock` mode pulls from this file. `retro` mode reads
it to find the repeated misses.

A question stays open until it is answered correctly twice, on two different dates.
Spaced repetition is the point. A question answered once is recognized, not known.

**Format:** one row per question.

| ID | Unit | Question | Level | Last asked | Result | State |
|---|---|---|---|---|---|---|
| Q-001 | 00 | Name the four properties a database row has that a Kafka partition does not, for dispatching work. Then name the workload where the answer flips. | derive | — | — | open |
| Q-002 | 00 | A worker publishes its result to Kafka inside the same try block that catches failures. What goes wrong, and what does the operator see in the console? | derive | — | — | open |
| Q-003 | 00 | Your error handler covers three exception types and all three are tested. Why is the contract still broken, and which request breaks it first? | defend | — | — | open |
| Q-004 | 00 | How do you tell whether a passing test can actually fail? Apply it to a test that says polling stops on a hidden tab. | derive | — | — | open |
| Q-005 | 00 | The integration test needs the SQL of another deployable. Why not depend on that module, and what does the chosen approach cost? | defend | — | — | open |
| Q-006 | 00 | The control plane enables virtual threads. Name the workload where that setting buys nothing, and say why. | recall | — | — | open |

**Level:** `recall` (a fact), `derive` (work it out), `defend` (justify under push-back).

**Result:** `pass`, `partial`, `fail`.

**State:** `open`, `learning` (one pass), `retired` (two passes).

---

## Miss log

Write the reason for every failure here. The reason is the curriculum, not the
question.

| Date | Question ID | Why it failed | Fix |
|---|---|---|---|
| — | — | — | — |
