# 04 — Deterministic Replay

When a workflow process crashes or returns, the JVM discards its stack frames and
local variables. Helios cannot save a running thread to disk. Instead it uses
**deterministic replay**: re-run the workflow code from its first line on every
turn, giving it a context that consults the event history rather than repeating
side effects.

---

## 1. The problem ordinary Java code cannot solve

A Java method runs, allocates locals, calls other methods, and returns. That
execution state is gone when it returns — or when the JVM crashes. There is no
built-in way to ask "which line were you on?" and resume from there.

Storing Java thread stacks to disk would couple Helios to one JVM version, break
across server restarts, and be unreadable by any monitoring tool. The event
history exists precisely so Helios never needs to persist a thread.

---

## 2. The key rule: replay from the first line

On every workflow turn, Helios executes the workflow definition from line 1.
It does not store an instruction pointer. Instead it gives the workflow a
**context** that changes behaviour based on what is already in the event history.

By the time the workflow reaches a decision point, the context has either
returned a previously recorded result (fast-forwarding through history) or
halted execution because new external work is needed.

---

## 3. ActivityScheduled vs ActivityCompleted

`ActivityScheduled` records **intent**: "the engine committed to requesting this work."

`ActivityCompleted` records an **observed outcome**: "an external source reported this result."

They are separate events, separated in time by real work performed outside the
engine. A history that has `ActivityScheduled` but no `ActivityCompleted` means
the run is mid-flight: request committed, answer not yet received.

---

## 4. What `context.executeActivity(type, input)` does

**Case A — scheduled and completed both in history:**
Return the recorded result immediately. No new command emitted. Replay advances.

**Case B — scheduled exists, no completion:**
The answer has not arrived yet. Suspend with no new commands. Wait for
`ActivityCompleted` to be appended before replaying again.

**Case C — no matching scheduled event at the history frontier:**
New decision. Emit `ScheduleActivity` command and suspend. The engine translates
it to `ActivityScheduled`, appends it, and waits for the external world.

---

## 5. Why activity ID is a deterministic call position

In this learning phase the context assigns IDs mechanically: `activity-1` for
the first call, `activity-2` for the second. The same workflow code always
reaches the same call in the same order, so the ID is always the same across
replays. The ID encodes *position* in the workflow, not external identity.
Stable IDs make history matching unambiguous.

---

## 6. Full three-turn LoanApproval example

**Turn 1 — start.**
Engine starts run `loan-A`, type `LoanApproval`, input `app-100`.
Appends `WorkflowStarted` at seq 1. Workflow code is not run yet.

**Turn 2 — first replay.** Replay re-runs `LoanApproval.execute(ctx)` from line 1.
`ctx.executeActivity("checkCredit", "app-100")` → history frontier has no schedule → **Case C**.
Emits `ScheduleActivity("activity-1", "checkCredit", "app-100")`, suspends.
Engine appends `ActivityScheduled{activity-1}` at seq 2.
External world appends `ActivityCompleted{activity-1, "score:750"}` at seq 3.

```
seq 1: WorkflowStarted   { LoanApproval, app-100 }
seq 2: ActivityScheduled { activity-1, checkCredit, app-100 }
seq 3: ActivityCompleted { activity-1, score:750 }
```

**Turn 3 — second replay.** Re-runs from line 1.
`ctx.executeActivity("checkCredit", "app-100")` → both scheduled and completed
present for `activity-1` → **Case A**. Returns `"score:750"` immediately, no command.
Workflow continues. `ctx.completeWorkflow("approved")` → emits `CompleteWorkflow`.
Engine appends `WorkflowCompleted{approved}` at seq 4.

```
seq 1: WorkflowStarted   { LoanApproval, app-100 }
seq 2: ActivityScheduled { activity-1, checkCredit, app-100 }
seq 3: ActivityCompleted { activity-1, score:750 }
seq 4: WorkflowCompleted { approved }
```

**Validation replay.** Any later replay fast-forwards through all four events
and returns `"approved"` with no new commands. History is final.

---

## 7. Determinism is a constraint, not a courtesy

Replay works only if the same code produces the same commands against the same
history. Changing an activity type, reordering calls, or branching on the wall
clock will cause replay to reach a decision expecting one thing and find another.
This is a **non-determinism error** the engine must surface loudly.

Safe workflow code must never call `System.currentTimeMillis()`,
`UUID.randomUUID()`, perform I/O directly, or read mutable global state. All
inputs must come from the context.

---

## 8. What Phase 3 still cannot do

- Persist history across a restart.
- Actually execute activities (the test harness appends `ActivityCompleted` manually).
- Retry failed work, schedule timers, or process signals.
- Execute concurrently or expose any API.

---

## Component flow

```
workflow source
    → replay executor   (re-runs from line 1 each turn)
    → context           (checks history on each executeActivity call)
    → new command(s)    (or no command when suspended)
    → Phase 2 history service  (validate → translate → append)
    → events in EventHistory
```

## History-driven fast-forward

```
history: ActivityScheduled{activity-1, checkCredit} + ActivityCompleted{activity-1, score:750}
    → replay calls ctx.executeActivity("checkCredit", "app-100")
    → context matches activity-1  (Case A)
    → returns "score:750" immediately, no ScheduleActivity emitted
    → workflow continues to next decision point
```

---

## Why suspension is not failure

A workflow that suspends has done exactly the right thing. It committed its
decision (or found it already recorded), advanced history by one fact, and
yielded control back to the engine. Suspension is the engine's rest state between
external events — not an error, not a timeout, not a blocked thread.

When `ActivityCompleted` arrives, the engine replays from scratch. The context
fast-forwards through all completed work and arrives at the former suspension
point — now with a recorded answer. The workflow continues as if it were never
interrupted.