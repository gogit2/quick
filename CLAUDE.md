# QuickFlow — agent guide

This project is **self-contained**. Everything an agent needs is inside this folder. Never read, write or depend on files outside it, and ignore any CLAUDE.md or settings from parent directories.

## What this repo is
- `requirements/`: PRD.md, task-brief.txt (mandatory assignment), DECISIONS.md (answers to clarifications; overrides the PRD where they conflict).
- `specs/NNN-*/`: Spec Kit output. `contracts/openapi.yaml` is the API contract and the single source of truth for BE and FE. `acceptance.md` lists the curl cases and Playwright scenarios per milestone.
- `loops/`: reusable Claude loops (`backend-dev`, `frontend-dev`, `verify`). The shared rules are in `loops/_shared/PROTOCOL.md`.
- `scripts/`: the bash runner (`run-loop.sh`, `pipeline.sh`, `verify.sh`) and helpers. The runner owns control flow, state, budget, servers and git commits.
- `backend/`: Spring Boot 3.5, Java 17, Maven wrapper, Flyway, H2 (file), springdoc.
- `frontend/`: Angular 19 + Angular Material, Node 18.
- `tools/`: pinned Playwright MCP 0.0.40 and Prism 5.12.0, plus project-local Chromium (`tools/.browsers`).
- `run-artifacts/`: sessions.csv, prompts, logs, Playwright output, Excel export.

## Hard rules for every agent
1. Follow `.specify/memory/constitution.md`.
2. Never `git commit`, `push`, `reset`, `checkout`, `tag`, `stash` or `clean`. Only the runner commits.
3. Never edit `specs/**/contracts/**`. Report contract problems in `loops/_shared/contract-issues.md`.
4. Start and stop servers only through `scripts/backend.sh`, `scripts/frontend.sh` and `scripts/mock.sh`, and always stop what you started.
5. Never ask the user a question in a headless loop. Use `requirements/DECISIONS.md`; otherwise record an assumption in `loops/_shared/assumptions.md` and continue.
6. Java 17 language level only. Node 18.20: Angular 19 max.

## Ports
| Port | Use |
|---|---|
| 8091 | backend self-check (backend-dev) |
| 8090 | stable backend (verify) |
| 4300 | frontend dev server |
| 4010 | Prism mock of the contract |
