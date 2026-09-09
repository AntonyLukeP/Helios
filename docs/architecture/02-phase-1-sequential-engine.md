# 02 â€” Phase 1: Sequential Engine Design

## What problem does Phase 1 solve?

Phase 1 adds one thing: a single entry point that accepts a workflow definition
and an input, executes the logic, and returns the result. Nothing is persisted;
nothing runs in the background. The goal is a correct, minimal call path.

---

## Three responsibilities

| Who | Owns |
|-----|------|
| `WorkflowDefinition<I, O>` | The business logic: what to do with input `I` to produce output `O`. |
| `HeliosEngine` | The invocation boundary: validate arguments, call the definition, return the result. Nothing else. |
| The caller | Constructing the input and handling the result or error. |

Each responsibility belongs to exactly one participant. The engine is deliberately
boring: it does not know what "greeting" or "loan approval" means.

---

## Minimal public API

```java
@FunctionalInterface
interface WorkflowDefinition<I, O> {
    O execute(I input);
}

final class HeliosEngine {
    <I, O> O run(WorkflowDefinition<I, O> workflow, I input) { ... }
}
```

`WorkflowDefinition` is a functional interface so a caller can pass a lambda
instead of writing a named class. The engine method is generic: it works for any
input and output type without knowing what those types are.

---

## Error semantics

**Null workflow definition:** programmer misuse â€” fail immediately with a clear
`NullPointerException("workflow definition must not be null")` before any
workflow code runs.

**Exception from the workflow:** propagate it unchanged to the caller. Durable
failure recording, retries, and compensation are deferred to a later phase.

---

## What Phase 1 deliberately cannot do

- Resume after a process restart â€” state lives only in memory.
- Wait for an external activity, timer, or human signal.
- Schedule work on a different thread or process.
- Run two workflow branches in parallel.
- Persist any event, command, or workflow history to a database.
- Retry a failed workflow automatically.
- Guarantee any delivery behavior.

These omissions are intentional. Correctness in the simple case must be proven
before the complex case is built.

---

## Example and call diagram

```java
WorkflowDefinition<String, String> greet = name -> "Hello, " + name + "!";
HeliosEngine engine = new HeliosEngine();
String result = engine.run(greet, "Alice");   // "Hello, Alice!"
```

```
Caller
  â”‚  engine.run(greet, "Alice")
  â–¼
HeliosEngine  â€” validates workflow != null
  â”‚  greet.execute("Alice")
  â–¼
WorkflowDefinition
  â”‚  returns "Hello, Alice!"
  â–¼
HeliosEngine  â†’  returns "Hello, Alice!"
  â–¼
Caller receives result
```


---

## Phase 1 limitations

The Phase 1 engine is a single method call: `workflow.execute(input)`. Every
limitation below follows directly from that fact.

**1. It cannot survive a restart.**
The workflow result exists only as a Java object in the JVM heap. When the
process stops — whether cleanly or by crash — that object is gone. There is no
database, no file, and no log that records what ran or what it returned. The
next process start has no memory of the previous one.

**2. It cannot pause and resume later.**
`execute` is a normal blocking method call. The engine waits for it to return
and then immediately returns the result to the caller. There is no mechanism to
suspend execution mid-workflow, park the state somewhere, and continue when an
external event arrives hours later. The entire workflow must complete in one
uninterrupted call.

**3. It cannot call an external API safely.**
If `execute` calls an HTTP endpoint, writes to a database, or sends an email,
the engine has no way to know whether that call succeeded, failed, or was
partially applied when the process crashed. There is no idempotency key, no
record of "this side effect already happened", and no retry boundary. The same
side effect could be triggered zero times (crashed before the call) or multiple
times (crashed after the call but before the caller received the result).

**4. It cannot retry a failed workflow.**
When `execute` throws an exception, the engine propagates it unchanged to the
caller and stops. There is no retry policy, no backoff, no dead-letter
destination, and no way to distinguish a transient network error from a
permanent business error. Retry is the caller's responsibility in Phase 1.

**5. It cannot provide any special guarantee for concurrent execution.**
`HeliosEngine` is stateless and thread-safe by default: two callers can call
`run` at the same time on different threads with no interference. However, if
two concurrent workflows share any mutable state outside themselves — a shared
Java object, a database row, an external counter — there is nothing in the
engine to coordinate or serialize that access. Isolation between concurrent
executions is entirely the caller's and the workflow's responsibility.

**6. It cannot provide an audit trail.**
There is no record of which workflow ran, when it ran, what input it received,
what result it returned, or whether it succeeded or failed. If a stakeholder
asks "what happened to loan #4872 on Tuesday?", Phase 1 has no answer. Every
event, decision, and outcome that a production workflow engine records in an
immutable history is absent here. That history is the central feature of Phase 2.
---

## Questions Phase 2 will answer

- How does the engine record that it *decided* to run a step, separately from
  the step running?
- What is a **command** (intent) versus an **event** (fact), and why does that
  distinction matter for crash recovery?
- If the process crashes between issuing a command and recording its result, how
  does the engine reconstruct what it was doing?
