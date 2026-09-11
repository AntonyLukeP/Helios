# 03 — Commands, Events, and History

In Phase 1 the engine runs workflow logic once and returns a result. It has no
memory between calls. Phase 2 introduces the three concepts that make memory
possible: commands, events, and an ordered history. This note explains each one
using the QuickLend loan-approval example.

---

## 1. A command is intent, not proof

When a workflow decides it is time to check an applicant's credit score, it
issues the command `ScheduleActivity("run-credit-check", applicantId)`. That
command is a *request*: "I want this to happen." It does not mean the activity
ran. It does not mean the result was received. It does not mean anything persisted.

A command lives only long enough to be translated into an event. The workflow
code produces commands; the engine consumes them. Commands do not enter the
event history directly.

---

## 2. An event is an immutable fact

Once the engine accepts a command, it records what *happened* as an event:
`ActivityScheduled("run-credit-check", applicantId, sequenceNo=3)`. That event
is a permanent, past-tense fact. It is never updated, never deleted, never
corrected. If it was wrong, a later event records the correction.

Immutability is not a rule imposed for cleanliness — it is what makes crash
recovery possible. An engine that restores state by reading a history can only
trust that history if every entry is a reliable, unmodified record.

---

## 3. Why `ScheduleActivity` becomes `ActivityScheduled`, not `ActivityCompleted`

QuickLend issues `ScheduleActivity("run-credit-check")`. The event recorded is
`ActivityScheduled` — not `ActivityCompleted`, not `ActivitySucceeded`.

This is deliberate. At the moment the engine records the scheduling event, the
activity has not run. A worker has not been assigned. The bureau has not been
called. Recording a *completion* event at scheduling time would be a lie, and a
lie in the history would corrupt every replay that depends on it.

The completion will be recorded later — in a separate `ActivityCompleted` event —
when a worker actually reports the result back to the engine.

---

## 4. Why a run needs a stable identifier

The workflow *type* is `loan-approval`. Many loans are in flight at once. The
engine needs to know which history belongs to which loan, independently of the
type name. A `runId` — a UUID assigned at start — provides that stable identity.

Alice's loan run is `run-7f3a…`. Bob's is `run-c91b…`. Both are `loan-approval`
workflows. Their histories are completely separate. Any event, command, or query
must carry the `runId` to be unambiguous.

---

## 5. Why events need an ordered sequence number

Events within one run are ordered by a monotonically increasing integer:
`sequenceNo`. The sequence number serves two purposes:

- **Ordering:** replay must apply events in the exact order they were recorded.
  A sequence number that is always incrementing guarantees that order even if
  two engine threads race to append at the same instant.

- **Optimistic concurrency:** when the engine appends event #5, it checks that
  the current last event is #4. If another process already wrote #5, the check
  fails and the engine retries. This is what makes each decision exactly-once.

---

## 6. Why history is append-only, even in this in-memory learning phase

Allowing history to be modified would mean that the state you observe by
replaying history might differ from the state that produced the original
decisions. That gap — between what happened and what the history says happened
— is the root cause of correctness bugs in workflow engines.

Even in Phase 2, where history lives only in memory and crashes lose everything,
the append-only rule is enforced. Practicing it now means the code will be
correct when a durable store is added later without needing structural changes.

---

## 7. Phase 2 flow

```
Caller:   engine.startRun("loan-approval", loanId)
Engine:   assign runId
          append  WorkflowStarted { runId, type, input, sequenceNo=1 }
          execute WorkflowDefinition.execute(input)
Workflow: produces Command: ScheduleActivity("run-credit-check", loanId)
Engine:   translate command → event
          append  ActivityScheduled { runId, name, input, sequenceNo=2 }
Caller:   engine.getHistory(runId)  →  [WorkflowStarted, ActivityScheduled]
```

The activity is not executed. The history is not persisted. This is the full
scope of Phase 2.

---

## Command → Event table

| Command | Meaning | Event recorded now | What does NOT happen yet |
|---|---|---|---|
| `ScheduleActivity` | Ask for outside work | `ActivityScheduled` | The activity is not executed |
| `CompleteWorkflow` | Request a successful close | `WorkflowCompleted` | Nothing is persisted |

---

## What is deliberately absent in Phase 2

- **Actual activity execution:** a worker process that claims and runs the task.
- **Database durability:** history lives in a `List<Event>` in memory; a restart
  loses everything.
- **Replay:** reading the history back and re-driving the workflow from it.
- **Retries:** no policy for re-scheduling a failed activity.
- **Time and timers:** no wall-clock waits, no escalation deadlines.
- **Task queues:** no mechanism to dispatch work to an external worker.
- **Network APIs:** no HTTP, gRPC, or messaging layer.

---

## Three rules that must hold from Phase 2 onward

1. **Commands do not enter history directly.** Only the events the engine
   derives from them do.
2. **Events are never modified after append.** If a fact changes, append a new
   event; never overwrite the old one.
3. **An activity-scheduling event is not an activity result.** `ActivityScheduled`
   records intent; `ActivityCompleted` records outcome. They are separate events,
   separated in time by actual work.