# Helios

**Durable Workflow Orchestration Engine for Regulated Business Processes**

Helios is a multi-tenant, self-hostable workflow orchestration engine designed for long-running business processes (loan approvals, KYC/AML checks, refund processing, subscription lifecycle management) that must survive crashes, retries, and deploys without losing state or auditability.

It occupies the gap between "roll your own with cron + Postgres" and "adopt Temporal" — lightweight, developer-friendly, and with first-class support for human-in-the-loop approval steps and a live operational dashboard.

---

## Current Phase

**Phase 0 — Repository Skeleton**
Project structure, documentation directories, and toolchain configuration only.
No engine code exists yet.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | **21 LTS** (Eclipse Temurin recommended) |
| Git | 2.x+ |

→ **[Getting started: clone, run tests, apply formatting](docs/getting-started.md)**

---

## Implementation

This project is built **entirely from scratch** — no scaffolding tools, no starter templates.
Every file is authored intentionally as part of the design and learning process.

---

## Documentation

| Directory | Purpose |
|---|---|
| `docs/architecture/` | System design documents, component diagrams, data flow |
| `docs/decisions/` | Architecture Decision Records (ADRs) |
| `docs/demo/` | Demo scripts, QuickLend persona walkthrough, screenshots |

---

## Quality Checks

Every change is verified by the same command, whether run locally or in CI:

```bash
# Linux / macOS / WSL
./gradlew check

# Windows (PowerShell or cmd)
.\gradlew.bat check
```

`check` runs two things in sequence:

| Step | What it does |
|---|---|
| `spotlessCheck` | Validates that all Java and Gradle files meet the project's formatting rules. Run `spotlessApply` first to fix any violations automatically. |
| `test` | Compiles and runs the full JUnit 5 test suite. Failed test names and full exception traces are printed to the console. |

**CI:** GitHub Actions runs `./gradlew check` on every push and pull request targeting `main`. The workflow is defined in [`.github/workflows/ci.yml`](.github/workflows/ci.yml). It contains no deployment, publishing, secrets, or external service connections — only validation.

---

## License

Private — not yet licensed for distribution.
