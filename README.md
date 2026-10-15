# QuickFlow, built with reusable Claude loops and Spec Kit

> Skeleton written in Phase A (setup). The full README, with Mermaid sequence diagrams of the loops, is completed in Phase D.

QuickFlow is a personal productivity app with tasks, habits, learning resources, time-boxed plans and a dashboard. The product requirements are in `requirements/PRD.md`.
It is built by three reusable headless Claude loops that a bash runner coordinates:

| Loop | Model | Job | Self-check / evidence |
|---|---|---|---|
| `backend-dev` | Haiku 5.5 | Spring Boot backend, built to `contracts/openapi.yaml` | `mvnw verify` (JUnit), curl suites per milestone, exported Swagger + diff |
| `frontend-dev` | Haiku 5.5 | Angular 19 + Material frontend, built to the same contract | `ng build`, Playwright MCP scenarios against a Prism mock |
| `verify` | Opus 5.5 | Checks a milestone on the real full stack, fixes small issues, otherwise rejects with FIX tasks | review, Swagger diff, curl regression, Playwright MCP (milestone + smoke) |

Planning (Spec Kit, interactive, Sonnet 5.5) produces one spec for the whole PRD, with milestones. Each milestone has `[BE]` and `[FE]` tasks.
After 3 failed trials, a milestone gets one retry on Sonnet 5.5. If that also fails, it is blocked.

## Quick start
```bash
(cd tools && npm ci && PLAYWRIGHT_BROWSERS_PATH=$PWD/.browsers node node_modules/playwright/cli.js install chromium)
scripts/pipeline.sh                      # run every pending milestone: BE || FE -> verify -> commit + tag
scripts/loopctl.py status                # where are we?
```

## Using a loop on its own
```bash
scripts/run-loop.sh --role backend-dev  --milestone M03          # one loop, until its part is built
scripts/run-loop.sh --role frontend-dev --ref US-TSK-02          # only the tasks of one story
scripts/run-loop.sh --role verify       --milestone M03          # one verification round
scripts/pipeline.sh --story "As a user, I want tags on tasks"    # new spec specs/00N-*, milestones continue
scripts/pipeline.sh --input path/to/other-PRD.md --yes           # whole new requirements file, no approval stop
```

## Running the application
Start the stable backend (port 8090) and the frontend dev server (port 4300) by hand. Run each command from the project root.
```bash
scripts/backend.sh start --port 8090 --db stable     # Spring Boot API on http://localhost:8090
scripts/frontend.sh start --api http://localhost:8090 # Angular on http://localhost:4300, proxies /api to the backend
# open http://localhost:4300
scripts/frontend.sh stop
scripts/backend.sh stop --port 8090
```
- Data: the H2 file database is `backend/data/stable.mv.db` (one file per `--db` name). `--fresh` on `backend.sh start` deletes it first.
- Pages (navigation order): Dashboard, Tasks, Habits, Learning, Plans, Settings. `/` opens the start page chosen in Settings.
- Demo clock: add `?clockOffsetMs=<ms>` to any URL to move the app clock, for example `http://localhost:4300/plans?clockOffsetMs=3600000` shows the app one hour ahead. Only the app clock moves, not the server clock.
- Build check: `cd frontend && npx ng build` (zero warnings expected).

## Layout
- `loops/<role>/`: `Loop-instructions.md`, `task.md`, `progress.md` (runner table: start/end, tokens, cost and session ids per milestone, plus the agent action log), `state/`, `outputs/` (one md per milestone with ticked tasks, plus curl / e2e / evidence).
- `loops/_shared/`: `PROTOCOL.md`, `budget.env`, `milestones.json`, `assumptions.md`, `contract-issues.md`.
- `scripts/`: runner, server scripts (PID files + health waits), `loopctl.py` (state, metrics, Excel), `split-tasks.py`.
- `run-artifacts/`: `sessions.csv`, `manual-prompts.csv`, prompts, logs, Playwright output, `summary.md`, the Excel file.

## Ports
| Port | Use |
|---|---|
| 8091 | backend self-check |
| 8090 | stable backend for verify |
| 4300 | frontend dev server |
| 4010 | Prism mock |

## Blocked milestones
`scripts/loopctl.py status` shows the reason. After resolving it (budget, contract, decision), reset the milestone's status in `loops/_shared/milestones.json` to `building` (and `upgraded`/`trial` if you want new trials), then re-run `scripts/pipeline.sh`. It resumes where it stopped.
