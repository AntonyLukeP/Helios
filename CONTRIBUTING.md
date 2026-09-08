# Contributing to Helios

Thank you for working on Helios. This document describes the **local pre-commit loop** every contributor must run before opening a pull request or pushing to a shared branch.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK  | **21 LTS** | Eclipse Temurin recommended |
| Git  | 2.x+    | |

---

## The Pre-Commit Loop

Run these three steps **in order** before every commit:

### Step 1 — Format

```
# Linux / macOS / WSL
./gradlew spotlessApply

# Windows (PowerShell or cmd)
.\gradlew.bat spotlessApply
```

Automatically fixes:
- Trailing whitespace on any line
- Missing newline at end of file
- Unused import statements in Java source

Run this **first**, before tests. Formatting fixes produce a clean diff; running tests first creates noise if Spotless later changes the same file.

---

### Step 2 — Test

```
# Linux / macOS / WSL
./gradlew test

# Windows
.\gradlew.bat test
```

Compiles all modules and runs every JUnit 5 test. On failure, the output prints:
- The full test display name
- The complete exception message and cause chain
- A trimmed stack trace pointing to the failing line

Fix failures before continuing.

---

### Step 3 — Inspect the diff

```
git diff
```

Read every changed line before staging it. `spotlessApply` can sometimes affect more files than expected; reading the diff catches unintended changes before they land in history.

---

## All-in-one verification

```
# Linux / macOS / WSL
./gradlew check

# Windows
.\gradlew.bat check
```

`check` is a Gradle lifecycle task that runs **both** formatting validation and tests in a single command. If `check` passes with exit code 0, your working tree is ready to commit.

> **Note**: `check` runs `spotlessCheck` (read-only: fails if files need formatting) — it does **not** apply fixes. Always run `spotlessApply` first, then `check`.

---

## Task Reference

| Command | What it does |
|---------|-------------|
| `spotlessApply` | **Rewrites** files to satisfy all formatting rules |
| `spotlessCheck` | **Validates** formatting — exits non-zero if any file is dirty; does not change files |
| `test` | Compiles and runs all unit tests |
| `check` | Runs `spotlessCheck` + `test` (and any future verification tasks added to the project) |

---

## Formatting Rules

Spotless is configured with a deliberately conservative rule set:

| Rule | Effect |
|------|--------|
| `removeUnusedImports` | Deletes import statements that are never referenced |
| `trimTrailingWhitespace` | Strips whitespace from the end of every line |
| `endWithNewline` | Ensures every file ends with exactly one newline (POSIX standard) |

**What Spotless does NOT do**: it does not reorder method bodies, change indentation width, wrap long lines, sort class members, or apply an opinionated style guide. If a `git diff` after `spotlessApply` looks larger than expected, check whether your editor is inserting tabs or extra blank lines.

---

## Adding a New Module

New modules are not added without a design discussion. When a module boundary is earned and agreed upon:

1. Create the directory under the repo root (e.g., `helios-worker/`).
2. Add `include 'helios-worker'` to `settings.gradle`.
3. Create `helios-worker/build.gradle` — the root `build.gradle` automatically applies shared conventions (Java 21 toolchain, Spotless, UTF-8 encoding, JUnit Platform).
4. Create `src/main/java/io/helios/worker/` and `src/test/java/io/helios/worker/`.
5. Run `./gradlew check` to confirm the new module integrates cleanly.

---

## Questions

Open a discussion or raise an issue rather than guessing at intent. The `docs/decisions/` directory will contain Architecture Decision Records (ADRs) explaining why things are the way they are.
