# Unit 00 — walkthrough

Only the non-obvious parts. The controllers, the entities, and the React table are
ordinary and are not discussed.

---

## 1. Why the broker does not dispatch the work

This is the decision an interviewer will press, because "put the jobs on Kafka" is the
common answer and it is wrong for this shape of work.

A Kafka consumer group gives one partition to one consumer. So the number of workers
that can run at the same time equals the number of partitions. A 200-worker fleet needs
at least 200 partitions, which makes the worker count a broker configuration decision.

Kafka commits offsets, not messages. An offset is a position in a log, so acknowledgement
is a range, not an item. There is no way to say "message 7 failed, give it to someone
else" while messages 8 and 9 succeed.

A job that runs for minutes exceeds `max.poll.interval.ms`. The broker then decides the
consumer is dead and moves the partition to another consumer, while the first one is
still working. The result is duplicate execution during a rebalance.

Kafka has no visibility timeout and no per-message backoff.

A database row has all four properties. A row can be claimed by exactly one worker, the
claim is a transaction, the claim can expire, and a single row can be retried without
touching its neighbors.

**When the answer flips.** Short, uniform tasks that need no individual redelivery, at a
volume the database cannot take. Then the broker is right and the database is the
bottleneck.

## 2. The try block that was too wide

The first version of the worker looked like this:

```java
try {
    String result = runner.run(payload);
    report(id, SUCCEEDED, result);      // inside the try
} catch (RuntimeException failure) {
    report(id, FAILED, failure.getMessage());
}
```

`report` publishes to Kafka. If that publish throws, the catch block runs, and the worker
publishes `FAILED` for a job whose work succeeded. The failure message of a Kafka
producer then becomes the business result of the job.

The rule this teaches: a try block covers the operation that can fail, and nothing after
it. Compute the outcome inside, act on the outcome outside.

```java
JobStatus status;
String result;
try {
    result = runner.run(payload);
    status = SUCCEEDED;
} catch (RuntimeException failure) {
    result = describe(failure);
    status = FAILED;
}
report(id, status, result);            // exactly once, outside
```

A second, smaller error was in the same place. `String.valueOf(failure.getMessage())`
writes the literal text `"null"` into the database when an exception carries no message.
`NullPointerException` usually carries no message.

## 3. A partial error contract is worse than none

The error handler covered three exception types. Everything else returned the default
Spring Boot body, which has the fields `timestamp`, `status`, `error`, and `path`.

The console has one type guard that accepts an RFC 9457 problem detail. That guard needs
`title`, `status`, and `detail`. The default body has none of them, so the guard rejected
it and the operator saw "Request failed with status 400" with no reason.

The trap: the three handled paths were tested and passed. The unhandled paths are the
ones a user meets first — a bad UUID in a URL, a malformed body, a wrong HTTP method.

Two changes fix it. `spring.mvc.problemdetails.enabled: true` makes Spring emit problem
details for the errors it raises before a controller runs. Extending
`ResponseEntityExceptionHandler` brings the framework exceptions into the same handler.

A contract that holds for the easy cases is a claim the code does not keep.

## 4. Do not rebuild what the library already does

The console had a hand-written `usePageVisible` hook, built on `useSyncExternalStore`
and the `visibilitychange` event, to stop polling on a hidden tab.

TanStack Query already does this. The query sets `refetchIntervalInBackground: false`,
and the library gates the interval on `focusManager.isFocused()`, which reads
`document.visibilityState !== 'hidden'`.

The hook was dead code, and its test passed with the hook deleted. That is the more
useful lesson: **a test that passes after you delete the thing it tests is not a test.**
The way to find out is to delete the mechanism and watch. If nothing goes red, the test
was describing the framework, not your code.

## 5. Why the integration test copies SQL instead of importing it

`JobLifecycleIT` runs in the control plane. It needs the exact claim statement of the
worker. Adding a dependency on the worker module would put both deployables on one
classpath, which defeats the boundary the unit exists to create.

So the test copies the two statements, and then reads `JobClaimRepository.java` and fails
if the copy has drifted. Duplication with a drift guard beats a dependency that erases
the boundary.

## 6. Virtual threads here, and why

The control plane sets `spring.threads.virtual.enabled=true`. Every request in it blocks
on Postgres. Virtual threads are cheap to park, so the container holds many blocked
requests without holding many platform threads.

They would not help if the work were CPU-bound. A virtual thread still needs a carrier
thread to run on, and the carrier count is the core count. Loom moves the cost of
waiting, not the cost of computing.

## 7. The three defects that were left in

| Defect | Why it is here | Closes in |
|---|---|---|
| The claim has no `SKIP LOCKED`, no lease, and no expiry. Two workers can take one row, and a dead worker strands a job. | Unit 01 opens with the test that proves the race. A correct skeleton would hide the problem the unit teaches. | 01 |
| The row write and the publish are not atomic. A crash between them loses the event. | Unit 03 adds the outbox. | 03 |
| A repeated completion event is rejected as an illegal transition, retried by Spring Kafka, then dropped. | Unit 03 adds the inbox. The noise is visible now, which is the point. | 03 |

A fourth was found during review and is also left: any worker id can complete a claimed
job, because the completion is not checked against the claim holder. That is fencing, and
it belongs to unit 01.
