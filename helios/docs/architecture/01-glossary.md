# 01 — Glossary

This glossary defines the terms used throughout Helios documentation and source code. Each definition is written for someone who has not used a workflow engine before. Where a term is used differently in other systems (Temporal, Step Functions, Conductor), that is noted.

Terms are listed in the order a new reader will encounter them, not alphabetically.

---

## Workflow Definition

**What it is:** A named, versioned description of a process — the template. It says "a loan approval consists of these steps, in this order, with these retry rules." It does not represent any particular running instance of that process.

**Example:** `loan-approval-v2` is a workflow definition. It describes the steps for approving a QuickLend loan. It is stored once and shared by every loan application.

---

## Workflow Run (Workflow Instance)

**What it is:** One concrete execution of a workflow definition, started for a specific piece of business data. A run has a unique ID, a status (running, paused, completed, failed, cancelled), and its own isolated event history.

**Example:** When Alice submits a loan application, Helios creates a new workflow run with ID `wf-alice-2024-001`. Bob's application creates a separate run `wf-bob-2024-002`. They share the same definition but are completely independent of each other.

---

## Command

**What it is:** An instruction that the workflow code issues to the engine: "schedule this activity," "start this timer," "wait for a signal." A command expresses **intent** — it says what should happen next. Commands are recorded as events once the engine accepts them.

**Example:** The workflow code reaches the step `creditBureauCheck()` and issues the command: "Schedule the `run-credit-check` activity for this workflow run." The engine records that command and dispatches a task to a worker.

> **Key distinction:** A command is a request issued *from* the workflow *to* the engine. An event is the engine's record that something *has happened*.

---

## Event

**What it is:** An immutable fact recorded in the event history. Events are always past tense: "activity was scheduled," "activity completed," "timer fired," "signal received." Once written, an event is never modified or deleted.

**Example:** After the credit-check activity finishes, the engine appends a `ACTIVITY_COMPLETED` event to the history, including the activity's output (the credit score). That event is now a permanent part of the record.

---

## Event History

**What it is:** The append-only, ordered list of all events that have occurred for a single workflow run. It is the **source of truth** — the engine never stores a "current state" field anywhere. Instead, it derives the current state at any time by replaying the event history from the beginning.

**Example:** The event history for Alice's loan run might look like:

```
1. WORKFLOW_STARTED         { input: { amount: 50000 } }
2. ACTIVITY_SCHEDULED       { name: "run-credit-check" }
3. ACTIVITY_TASK_STARTED    { worker: "worker-3", lease_expires: ... }
4. ACTIVITY_COMPLETED       { output: { credit_score: 720 } }
5. TIMER_STARTED            { fire_at: "+48h" }
6. SIGNAL_RECEIVED          { signal: "underwriter-decision", payload: { approved: true } }
7. ACTIVITY_SCHEDULED       { name: "disburse-funds" }
...
```

---

## Activity

**What it is:** A discrete, isolated unit of work that the engine delegates to an external worker process. Activities contain the code that actually *does* things: calls an API, queries a database, sends an email. The engine itself never executes activity code — it only schedules, tracks, and retries it.

**Example:** `run-credit-check`, `send-notification`, and `disburse-funds` are all activities in the QuickLend workflow. Each is implemented as a Java method in a worker process. The engine schedules them; the worker executes them.

> **Important:** Because workers can crash, the engine may schedule the same activity more than once. Activity implementations must be **idempotent** (see below).

---

## Workflow Task

**What it is:** A unit of work handed to the engine's own internal logic (not an external worker) to make progress on a workflow: decide the next step, apply a signal, fire a timer. Workflow tasks are short and purely computational — they replay the event history to decide what command to issue next.

**Example:** After a `SIGNAL_RECEIVED` event is appended, the engine issues a workflow task to itself: "replay the history for `wf-alice-2024-001` and decide what to do next." The outcome is a new command: "schedule the `disburse-funds` activity."

---

## Activity Task

**What it is:** A unit of work placed in the task queue for an external worker to claim and execute. It contains everything the worker needs: which activity to run, its input arguments, the idempotency key, and a lease deadline.

**Example:** When the engine schedules the `run-credit-check` activity, it creates an activity task in the `tasks` table. A worker polls the table, claims the task (setting `status = CLAIMED` and recording its lease expiry), runs the activity, and reports the result back to the engine.

---

## Replay

**What it is:** The process of reconstructing a workflow's current state by re-executing the workflow definition code against its recorded event history, from the first event to the most recent. The engine uses replay every time it needs to decide what to do next for a workflow run.

**Example:** The engine needs to decide what to do after Alice's underwriter approves the loan. It replays the event history: starts the workflow, sees the credit check was completed, sees the timer was started, sees the signal was received with `approved: true`, and from that replay determines: "the next step is to schedule `disburse-funds`."

Replay is what makes crash recovery free: after a server restart, the engine simply replays the history and continues from the last recorded event — no manual state recovery needed.

---

## Determinism

**What it is:** The property that the workflow definition code, when replayed against the same event history, always produces exactly the same sequence of commands. If the code is deterministic, replay will always reconstruct the same state — making crash recovery and auditing reliable.

**Example:** A workflow that calls `Math.random()` or `LocalDate.now()` is **non-deterministic**: different replays will produce different decisions, breaking the replay guarantee. The correct pattern is to pass the current date in as an input, or to record the result of `Math.random()` as an event on first execution and read that recorded result during replay.

> **Rule:** Workflow definition code must never read from the real world (clocks, random numbers, external APIs, databases) during replay. It may only read from the event history.

---

## Idempotency Key

**What it is:** A stable, unique identifier attached to an activity task so that the activity implementation can detect and safely skip a duplicate invocation. If the engine delivers the same task twice, the worker can use the idempotency key to check "have I already done this?" and return the cached result without performing the side effect again.

**Example:** The `disburse-funds` activity task carries the idempotency key `disburse-wf-alice-2024-001-step-7`. Before charging the customer, the payment processor checks its own records: "has a disbursement with this key already succeeded?" If yes, it returns the original result. No duplicate charge.

---

## Lease

**What it is:** A time-limited claim on a task. When a worker picks up an activity task, it sets a `lease_expires_at` timestamp. The engine treats the task as exclusively owned by that worker until the lease expires. If the worker crashes or goes silent, the lease expires and the engine reassigns the task to another worker.

**Example:** Worker-3 claims a credit-check task at 14:00 with a lease expiring at 14:05. Worker-3 crashes at 14:03. At 14:06, the engine scans for expired leases, finds the unclaimed task, and reassigns it to Worker-7.

> **This is why activities are at-least-once:** the engine cannot know whether the crashed worker finished the work before crashing. It must retry to be safe.

---

## Optimistic Concurrency

**What it is:** A technique for preventing two processes from simultaneously modifying the same data, without holding a lock for the entire duration of the operation. Each event in the history has a sequence number. Before appending a new event, the engine checks that the current last sequence number matches what it expected. If another process has appended an event first (the numbers don't match), the operation is rejected and retried.

**Example:** Two engine replicas both try to append event #5 to Alice's history at the same time. The database's `UNIQUE (workflow_id, sequence_no)` constraint ensures only one insert succeeds. The other replica gets a conflict error, re-reads the history, and retries — now appending event #6 with correct information.

> **This is what makes workflow decisions exactly-once:** only one append can win per sequence number, so only one replica can make each decision.

---

## Timer

**What it is:** An instruction to pause a workflow until a specific wall-clock time has passed, then resume automatically. Timers survive server restarts because the engine persists the target fire time in the database.

**Example:** After sending a loan application to the underwriter, the workflow starts a 48-hour timer: "if no signal arrives before this timer fires, escalate." A timer-scanning process polls the database for due timers and delivers a `TIMER_FIRED` event to the workflow, triggering the escalation path.

---

## Signal

**What it is:** A named, external message delivered to a running (or paused) workflow instance. A workflow can wait for a specific signal, and will not proceed until that signal arrives or a timeout occurs. Signals are how humans and external systems communicate decisions into a running workflow.

**Example:** The QuickLend dashboard lets an underwriter click "Approve" or "Reject." That button press calls the Helios signal API: `POST /v1/workflows/wf-alice-2024-001/signal` with body `{ "signal": "underwriter-decision", "payload": { "approved": true } }`. The engine appends a `SIGNAL_RECEIVED` event and resumes the workflow from its waiting state.

---

## Cancellation

**What it is:** An external instruction to stop a workflow run that is currently in progress. Cancellation is **cooperative** — the engine stops scheduling new activities and records a `CANCELLATION_REQUESTED` event, but any activity that is already running on a worker is allowed to finish (or be abandoned when its lease expires).

**Example:** Alice withdraws her loan application. The caller sends `POST /v1/workflows/wf-alice-2024-001/cancel`. The engine records the cancellation, stops the timer, and moves the run to `CANCELLED` status. If the credit-check activity was already running, the engine does not kill that process — it simply does not schedule the next step after the result arrives.

---

## Compensation

**What it is:** A set of activities that undo or reverse the side effects of earlier activities in a workflow, executed when the workflow encounters an unrecoverable error or is cancelled after partial progress. This is the Saga pattern applied to workflow execution.

**Example:** Alice's loan is approved and funds are disbursed. The notification step then fails with an unrecoverable error. Helios triggers a compensation workflow that calls `reverse-disbursement` to return the funds — because doing nothing would leave Alice charged but without a confirmed loan.

> **Phase 1 note:** Basic compensation steps are in scope; full saga orchestration with complex branching is a later-phase feature.

---

## Tenant

**What it is:** An isolated organizational account within a single Helios deployment. Every workflow definition, workflow run, task, and event belongs to exactly one tenant. Tenant boundaries are enforced at the data-access layer — one tenant cannot see or affect another's data.

**Example:** QuickLend is Tenant A. A separate company, MegaBank, onboards as Tenant B on the same Helios deployment. MegaBank's underwriters see only MegaBank's loan applications; they cannot see or signal QuickLend's workflows.

---

## Namespace

**What it is:** A named grouping within a tenant that organises workflow definitions and runs. A tenant might use different namespaces for different business domains or environments (e.g., `lending`, `onboarding`, `staging`).

**Example:** QuickLend uses namespace `lending` for all loan-approval workflows and namespace `hr` for employee onboarding workflows. Both namespaces belong to the same QuickLend tenant, but they are logically separated so that a search for "stuck workflows" in `lending` does not return `hr` results.

> **Phase 1 note:** Tenant isolation is a hard requirement from the start. Namespace support may be simplified in early phases — every tenant will have at least one default namespace.
