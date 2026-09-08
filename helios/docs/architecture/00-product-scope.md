# 00 — Product Scope

## The Problem

Most companies that handle regulated, multi-step business processes end up building the same fragile infrastructure by hand: a `status` column in a database table, a cron job that polls that table, ad-hoc Slack messages when something needs a human decision, and a lot of retry logic written in application code.

This works until it doesn't. A server crashes mid-refund. A cron job fires twice. An approver approves a request that was already cancelled. The company's audit team asks "what happened to loan #4872 on Tuesday?" and nobody has a clean answer.

The root cause is that **long-running processes are fundamentally different from request/response operations**. A loan approval might take three days. A KYC check might wait for a third-party API that is currently down. An employee onboarding might pause at "manager approval" for a week. These processes must survive server restarts, deploys, database failovers, and human delays — and they must leave behind a complete, tamper-evident history of every decision made.

Helios provides a single, general-purpose runtime for these processes. You define a workflow as code; Helios guarantees that it runs to completion, remembers exactly where it was, and never silently loses state.

---

## Concrete Example: QuickLend Loan Approval

QuickLend is a fictional fintech that approves personal loans. Their loan approval process looks like this:

1. Applicant submits a loan application.
2. An automated credit-bureau check runs (external API call).
3. If the credit score is below a threshold, a human underwriter must review the application within 48 hours.
4. If no underwriter acts within 48 hours, the application is automatically escalated to a senior underwriter.
5. The underwriter either approves or rejects the loan.
6. If approved, a fund-disbursement call is made to the payment processor.
7. The applicant is notified of the outcome.

Without Helios, QuickLend implements this as five separate services, two cron jobs, a status column with twelve possible values, and a lot of defensive code around "what if the payment processor times out after charging the customer but before confirming success?"

With Helios, this is one workflow definition. Each of the seven steps above is an **activity** (a discrete unit of work the engine delegates to a worker). The 48-hour wait is a **timer**. The underwriter decision is a **signal** (an external event delivered to a paused workflow). The engine persists every step outcome; if the server running the workflow crashes between step 5 and step 6, the engine resumes at step 6 on restart — with no duplicate fund disbursement.

---

## MVP Capabilities

These are the capabilities Helios will provide in its first working implementation.

| Capability | What it means |
|---|---|
| **Sequential workflows** | A workflow runs a list of activities in a defined order, with branches based on activity outcomes. |
| **Durable history** | Every decision and activity result is appended to an immutable event log. The engine reconstructs workflow state by replaying that log — never by reading a mutable "current state" row. |
| **Activities** | Discrete units of work executed by a worker process outside the engine. Activities are retried on failure according to a configurable retry policy. |
| **Timers** | A workflow can pause execution and resume after a wall-clock duration (e.g., "wait 48 hours then escalate"). Timers survive server restarts. |
| **Signals / approvals** | An external system or a human operator can deliver a named event (a signal) to a paused workflow, allowing it to resume with the signal's payload. This is the mechanism behind human-in-the-loop approval steps. |
| **Query and history API** | Callers can query the current projected state of a workflow instance and retrieve its full event history for audit purposes. |
| **Cancellation** | A running workflow can be cancelled by an external caller. The engine stops scheduling new activities and records a `CANCELLED` event. |

---

## Explicit Non-Goals for the First Implementation

The following are **out of scope for Phase 1** and will not be built until a later phase has earned the additional complexity.

| Not building yet | Why it is deferred |
|---|---|
| **Kafka** | The task queue will use Postgres with `SELECT ... FOR UPDATE SKIP LOCKED`. Kafka is added only when throughput proves that Postgres polling is the bottleneck — not before. |
| **Redis** | No distributed caching or Redisson-based distributed locking in Phase 1. The timer service will use a simple Postgres-based leader election approach. |
| **Kubernetes / container orchestration** | Deployment tooling comes after the engine is correct. Running locally with `java -jar` and `docker-compose` is sufficient for Phase 1. |
| **Multi-region / active-active** | Correctness within a single region is hard enough. Cross-region failover is a Phase N concern. |
| **Visual workflow designer** | The workflow definition format (YAML or code-based DSL) is the authoring surface. A drag-and-drop UI is a product feature, not an engine feature. |
| **Parallel / fan-out DAG execution** | Phase 1 supports strictly sequential workflows. Parallel branches (fan-out/fan-in) require additional scheduler complexity and are deferred. |
| **Exactly-once external side effects** | See the honesty note below. |

---

## Honesty Note: Exactly-Once Decisions vs. At-Least-Once Activities

This distinction is critical and must be understood before building anything.

**Workflow decisions are exactly-once.** When the engine decides "the next step is to run activity `disburse-funds`", it records that decision as an event in the database within a single transaction, protected by optimistic concurrency control (a sequence-number check). No matter how many engine replicas are running, only one of them can successfully append that event. The decision is made exactly once.

**Activity execution is at-least-once.** Once the engine has recorded the intent to run an activity, it hands a task to a worker. That worker might execute the activity successfully and then crash before reporting the result back to the engine. The engine, seeing no result and an expired lease, will hand the same task to another worker. The activity therefore runs again. This is unavoidable without coordinating across two independent systems (the engine database and the external system the activity calls) in the same transaction — which is generally impossible.

**The consequence:** every activity implementation must be idempotent. Running the same activity twice with the same inputs must produce the same outcome and leave the external system in the same state. For QuickLend, the fund-disbursement activity must check "has this disbursement already been made?" before charging the customer — not assume it is the first caller.

Helios provides an **idempotency key** on every activity task to make this check straightforward. But the responsibility for using that key correctly belongs to the activity implementation, not to the engine.
