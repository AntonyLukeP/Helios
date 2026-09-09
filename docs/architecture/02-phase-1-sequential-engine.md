# 02 — Phase 1: Sequential Engine Design

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

**Null workflow definition:** programmer misuse — fail immediately with a clear
`NullPointerException("workflow definition must not be null")` before any
workflow code runs.

**Exception from the workflow:** propagate it unchanged to the caller. Durable
failure recording, retries, and compensation are deferred to a later phase.

---

## What Phase 1 deliberately cannot do

- Resume after a process restart — state lives only in memory.
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
  │  engine.run(greet, "Alice")
  ▼
HeliosEngine  — validates workflow != null
  │  greet.execute("Alice")
  ▼
WorkflowDefinition
  │  returns "Hello, Alice!"
  ▼
HeliosEngine  →  returns "Hello, Alice!"
  ▼
Caller receives result
```

---

## Questions Phase 2 will answer

- How does the engine record that it *decided* to run a step, separately from
  the step running?
- What is a **command** (intent) versus an **event** (fact), and why does that
  distinction matter for crash recovery?
- If the process crashes between issuing a command and recording its result, how
  does the engine reconstruct what it was doing?
