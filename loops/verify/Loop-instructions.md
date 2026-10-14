# Loop: verify

**Role**: decide whether a milestone is really done, on the **real full stack**, after both backend-dev and frontend-dev reported `built`.
**Input**: the milestone id (in `task.md`), the spec folder, the git diff since the previous milestone tag, both loops' milestone files and evidence.
**Output**: `outputs/Mxx-round-<n>.md` (report), `outputs/evidence/Mxx/round-<n>/` (curl logs, screenshots), and `state/verdict.json`.

Follow `loops/_shared/PROTOCOL.md` for reading order, the action log and handoff. Instead of `state/result.json` you write `state/verdict.json`.

## Environment (already started by the runner; do not restart unless a fix needs it)
- Stable backend on **http://localhost:8090** from a fresh `stable` database (`run-artifacts/backend-latest.jar`).
- Frontend dev server on **http://localhost:4300**, proxying to :8090.
- If you change backend code, restart with `scripts/backend.sh restart --port 8090 --db stable --fresh` (rebuilds). Frontend changes hot-reload.

## Allowed to edit
`backend/**`, `frontend/**`, `loops/verify/**`, `loops/_shared/contract-issues.md` (status/resolution), `loops/_shared/assumptions.md` (append). Never the contract unless resolving a contract issue that is small and backward compatible (then log it as `resolved-by-verify`). Never commit.

## Checks (all of them, in order)
1. **Review**: `git diff <previous tag>..` (given in task.md) plus untracked files. Check against the contract, the constitution, DECISIONS.md, and the milestone's tasks. Every task ticked by the loops must really exist in the code.
2. **Swagger**: `scripts/export-swagger.sh --base http://localhost:8090 --ms Mxx --out loops/verify/outputs/evidence/Mxx/round-<n>`; the operations of this and earlier milestones must match the contract.
3. **Backend tests**: `cd backend && ./mvnw -q verify`.
4. **curl**: `scripts/curl-regression.sh --base http://localhost:8090 --upto Mxx --log loops/verify/outputs/evidence/Mxx/round-<n>/curl.log`.
5. **Browser (Playwright MCP)** against http://localhost:4300 on the real backend: this milestone's scenarios from `acceptance.md` / `loops/frontend-dev/outputs/e2e/Mxx.md` **end to end with real state**, then every `[smoke]` scenario of earlier milestones. For each: screenshot into the round evidence folder, `browser_console_messages` must show no errors, and no 5xx in `browser_network_requests`.
6. Contract issues of this milestone in `loops/_shared/contract-issues.md`: resolve or escalate.

## Small fixes (do them yourself)
Allowed only if **all** hold: ≤ 3 files, ≤ ~30 changed lines in total, no design change, no contract change (except a resolved small contract issue). Log every fix in the report (file, what, why) and **re-run the affected checks**. Anything bigger becomes a FIX task.

## Verdict
`PASS` only if every check passes (after your small fixes). Otherwise `REJECT` with FIX tasks precise enough for a Haiku loop: file/area, expected vs actual, evidence path, which check proves it.

`state/verdict.json` (exact shape, last thing you write):
```json
{
  "milestone": "M03",
  "round": 1,
  "verdict": "PASS | REJECT",
  "checks": { "review": "pass|fail", "swagger": "pass|fail", "unit": "pass|fail", "curl": "pass|fail", "e2e": "pass|fail", "smoke": "pass|fail|n/a" },
  "fixes": [ { "file": "backend/...", "summary": "...", "lines": 4 } ],
  "fix_tasks": [ { "role": "backend-dev | frontend-dev", "text": "...", "evidence": "loops/verify/outputs/evidence/M03/round-1/..." } ],
  "contract_issues": [ { "id": "CI-2", "status": "resolved-by-verify | escalated" } ],
  "escalate": false,
  "notes": "..."
}
```
Set `escalate: true` only for problems a loop cannot solve (contract redesign, contradiction in requirements); explain in notes.

## Report `outputs/Mxx-round-<n>.md`
Header (milestone, round, verdict, time), a table of checks with evidence links, "Fixes applied", "FIX tasks", "Contract issues", "Notes".
