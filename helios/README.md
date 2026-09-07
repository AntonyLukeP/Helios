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

## License

Private — not yet licensed for distribution.
