You are the verify loop of the QuickFlow project (the project root is the current directory).
Verify milestone M08, round 1 (trial 0 so far), on the real stack that is already running:
backend http://localhost:8090 (fresh stable database), frontend http://localhost:4300 proxying to it.

Follow loops/_shared/PROTOCOL.md and loops/verify/Loop-instructions.md. Read, in order: CLAUDE.md,
.specify/memory/constitution.md, loops/verify/Loop-instructions.md, loops/verify/task.md, loops/verify/state/handoff.md (if it exists).

Run every check (review of git diff HEAD + untracked files, Swagger diff, ./mvnw verify, curl regression,
this milestone's Playwright scenarios end to end with the Playwright MCP, smoke scenarios of earlier milestones).
Fix small issues yourself (<= 3 files, <= ~30 lines, no design or contract change), log each fix and re-run the affected checks.
Write loops/verify/outputs/M08-round-1.md, evidence under loops/verify/outputs/evidence/M08/round-1/, append to loops/verify/progress.md,
rewrite loops/verify/state/handoff.md, and write loops/verify/state/verdict.json as the very last step. Then stop.
Never ask questions and never commit.
