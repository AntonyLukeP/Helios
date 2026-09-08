# Getting Started with Helios

## What is Helios right now?

Helios is a durable workflow orchestration engine — but not yet.

At **Phase 0**, this repository is a clean, well-structured Java project foundation. The build toolchain is configured, the test runner works, code formatting is enforced, and CI is wired up. There is no workflow engine code yet. The first working engine concept (an in-memory workflow with commands and events) is the goal of Phase 1.

This guide helps you get the project running on your machine and understand how to work inside it productively. Every command shown here works right now, today, against the current codebase.

---

## Prerequisites

You need exactly two things installed:

| Tool | Minimum version | How to check |
|---|---|---|
| **Java JDK** | **21 LTS** | `java -version` |
| **Git** | 2.x+ | `git --version` |

Gradle is **not** required as a separate installation. The repository ships with a Gradle Wrapper (`gradlew` / `gradlew.bat`) that downloads the correct Gradle version automatically on first use.

> **Recommended JDK:** Eclipse Temurin 21, available free from [adoptium.net](https://adoptium.net). Any other JDK 21 distribution (Liberica, Microsoft, Amazon Corretto) also works.

---

## Step 1 — Get the code

Clone the repository and move into the project directory:

```bash
# Linux / macOS / WSL
git clone <repository-url> helios
cd helios

# Windows (PowerShell)
git clone <repository-url> helios
cd helios
```

If you have already cloned the repository, just navigate to where you put it:

```bash
# Linux / macOS / WSL
cd /path/to/helios

# Windows (PowerShell)
cd C:\path\to\helios
```

---

## Step 2 — Verify Java

Before running any build commands, confirm you have Java 21:

```bash
java -version
```

You should see output similar to:

```
openjdk version "21.0.x" ...
OpenJDK Runtime Environment Temurin-21.0.x+...
```

The major version number must be `21`. If it says `17`, `11`, or `24`, see the [Troubleshooting](#troubleshooting) section below.

> **Windows note:** If `JAVA_HOME` points to a different JDK than the one on your `PATH`, Gradle may use the wrong one. Set `JAVA_HOME` to your JDK 21 directory if builds behave unexpectedly.

---

## Step 3 — Run the tests

```bash
# Linux / macOS / WSL
./gradlew test

# Windows (PowerShell or cmd)
.\gradlew.bat test
```

**First run only:** Gradle will download its own distribution (~120 MB) and then download the test dependencies (JUnit 5, AssertJ) from Maven Central. This takes 30–90 seconds depending on your connection. Subsequent runs use the local cache and are much faster.

Expected output:

```
> Task :helios-core:compileJava
> Task :helios-core:compileTestJava
> Task :helios-core:test

ProjectInfo constants > NAME equals the canonical project identifier 'helios'      PASSED
ProjectInfo constants > VERSION_MAJOR is 0 — no stable release has been cut yet   PASSED
ProjectInfo constants > VERSION_MINOR is 1 — initial development phase has begun   PASSED

BUILD SUCCESSFUL in Xs
```

If you see `BUILD SUCCESSFUL`, everything is working correctly.

---

## Step 4 — Run all quality checks

`check` runs both the test suite and the formatting validator in one command:

```bash
# Linux / macOS / WSL
./gradlew check

# Windows
.\gradlew.bat check
```

If `check` exits with `BUILD SUCCESSFUL`, the code is correct and properly formatted. This is the same command GitHub Actions runs in CI on every push.

---

## Step 5 — Apply automatic formatting

If you have edited any `.java` or `.gradle` file and `check` fails with a formatting violation, fix it automatically:

```bash
# Linux / macOS / WSL
./gradlew spotlessApply

# Windows
.\gradlew.bat spotlessApply
```

This rewrites files in place to fix trailing whitespace, missing end-of-file newlines, and unused imports. It does **not** restructure your code or change indentation style.

Run `check` again after `spotlessApply` to confirm the violation is resolved.

---

## Step 6 — Inspect Git changes before committing

Before staging any file, read the diff:

```bash
git diff
```

This shows exactly what changed, line by line. `spotlessApply` sometimes modifies more files than you expect (for example, if a collaborator left trailing whitespace in a file you opened). Reading the diff prevents unintended changes from landing in history.

When you are satisfied:

```bash
git add -p          # stage changes interactively, one chunk at a time
git commit -m "your message"
```

---

## Troubleshooting

### Wrong Java version

**Symptom:** `java -version` shows a version other than 21, or Gradle prints a toolchain error.

**Fix (Linux / macOS):** Use a version manager like [SDKMAN](https://sdkman.io) to install and switch to Java 21:
```bash
sdk install java 21-tem
sdk use java 21-tem
```

**Fix (Windows):** Download Eclipse Temurin 21 from [adoptium.net](https://adoptium.net), install it, then update `JAVA_HOME` and your `PATH` in System Properties → Environment Variables.

After changing the JDK, open a new terminal and confirm with `java -version`.

---

### Gradle Wrapper download fails

**Symptom:** Running `./gradlew` prints a network error or `Unable to tunnel through proxy`.

**Cause:** The Gradle Wrapper needs to download `gradle-8.10.2-bin.zip` from `services.gradle.org` on first use. This requires internet access.

**Fix:** Check your internet connection or corporate proxy settings. If your network blocks `services.gradle.org`, ask your IT team to whitelist it, or download the distribution manually and place it in the local Gradle cache directory (`~/.gradle/wrapper/dists/`).

---

### A test fails

**Symptom:** `BUILD FAILED` with output showing `FAILED` next to a test name, followed by an exception.

**What to read:** The test output prints the full exception message, cause chain, and a stack trace pointing to the failing line. Read those carefully — the failure message is designed to tell you exactly what the actual vs. expected value was.

**Common cause in Phase 0:** You have modified `ProjectInfo.java` and the constants no longer match what the tests expect. Either revert your change or update the test to match the new intended value.

---

### Formatting check fails (`spotlessCheck`)

**Symptom:** `BUILD FAILED` with output containing `Step 'spotlessJava' found problem`.

Spotless prints a diff showing exactly which lines in which files do not meet the rules.

**Fix:** Run `spotlessApply` to correct the files automatically, then run `check` again:

```bash
# Linux / macOS / WSL
./gradlew spotlessApply && ./gradlew check

# Windows
.\gradlew.bat spotlessApply; .\gradlew.bat check
```

If `spotlessApply` itself fails, check whether the file has a syntax error — Spotless cannot parse malformed Java.

---

## How we build Helios

### One phase at a time

Helios is built incrementally. Each phase has a defined goal, and no phase adds complexity beyond what its goal requires. This keeps the codebase understandable at every point in time and ensures that every piece of code earns its place.

Phase 0 (now) establishes the build foundation. Phase 1 will introduce the first engine concepts. Later phases will add persistence, HTTP APIs, workers, timers, and the dashboard — in that order, each building on what came before.

### Tests first for bugs and invariants

Tests in this project are not written to hit a coverage number. They are written for two specific purposes:

1. **Documenting invariants** — the things that must always be true (e.g., "the project name is 'helios'", "a workflow cannot transition to COMPLETED from CANCELLED"). A test that documents an invariant is a machine-readable specification.

2. **Catching regressions** — if a later change accidentally breaks something that was correct before, the test fails loudly. This is especially important in a system where many components interact.

If you are not sure whether a piece of code needs a test, ask: "Is there something about this code that must always be true, and would be non-obvious to a reader six months from now?" If yes, write a test.

### No copying from reference implementations

Helios is built from first principles. Temporal, AWS Step Functions, Camunda, and Netflix Conductor are legitimate reference points for understanding the *problem domain* — they exist to validate that the problem is real and that the design decisions are defensible. But implementation code, test code, and configuration from those systems is never copied into this repository.

Everything in Helios is written by understanding the requirement and then implementing it. This is intentional: the engineering depth comes from reasoning through the design yourself, not from transcribing someone else's solution.

---

## What comes next: Phase 1 preview

Phase 1 introduces the first working piece of the engine. Specifically:

- **Workflow** — a named, versioned process definition that can be instantiated.
- **Command** — an instruction issued by workflow code to the engine ("schedule this activity", "start this timer").
- **Event** — an immutable fact appended to the workflow's history ("activity was scheduled", "activity completed").
- **In-memory execution** — the engine applies commands and records events entirely in memory. There is no database yet.

Phase 1 will prove the core correctness invariant: given the same event history, the engine always reconstructs the same workflow state. Everything in later phases (crash recovery, persistence, exactly-once delivery) depends on this being true.

**What Phase 1 will not include:** databases, HTTP APIs, workers, timers, signals, multi-tenancy, Redis, Kafka, Kubernetes, or a dashboard. Those arrive in their own phases, each after the phase before it has proven its correctness.
