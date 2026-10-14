# Loop protocol (shared by every loop)

One iteration = one fresh headless Claude session. You remember nothing from earlier sessions; **all memory is in files**. Do your work, write the files below, and exit. The bash runner decides what happens next.

## 1. Read first, in this order
1. `CLAUDE.md` and `.specify/memory/constitution.md`
2. Your `loops/<role>/Loop-instructions.md`
3. Your `loops/<role>/task.md` (written by the runner: spec, milestone, trial, model, budget, ref filter)
4. Your `loops/<role>/state/handoff.md` (notes from your previous iteration, if any)
5. The milestone file named in task.md (`loops/<role>/outputs/Mxx-*.md`)
6. As needed: `requirements/DECISIONS.md`, the spec folder (`spec.md`, `plan.md`, `data-model.md`, `contracts/openapi.yaml`, `acceptance.md`), `loops/_shared/assumptions.md`, `loops/_shared/contract-issues.md`

Read only what you need. Do not re-read whole folders.

## 2. Pick work
- **FIX tasks first** (section "FIX tasks" at the top of the milestone file), then unchecked tasks in file order.
- Take at most **5 tasks** per iteration (fewer if they are large). If task.md has a `ref` filter, take only tasks tagged with that ref.
- Respect `depends on` notes. Do not start tasks of another milestone.

## 3. Do the work
- Stay inside the folders your Loop-instructions allow.
- Build to the contract exactly: paths, methods, field names, enums, status codes, error shape.
- Ambiguity: check `requirements/DECISIONS.md`. If it is not settled there, append an entry to `loops/_shared/assumptions.md` and continue:
  `- A-<role>-<Mxx>-<n> (<ISO time>): <question> → <assumption taken> (affects: <files>)`
- Contract looks wrong or incomplete: append to `loops/_shared/contract-issues.md` (format inside that file), keep building to the contract as written, continue.
- Never ask the user anything. Never commit. Never edit `specs/**/contracts/**`, `scripts/**`, `.claude/**`, `loops/_shared/milestones.json`, or another loop's folder.

## 4. Self-check
Run the self-check in your Loop-instructions **before** you mark a task done. A task is done only when its code exists and the relevant check passes.
If you finished the last open task, run the **full** milestone self-check.

## 5. Write your files (always, even if you failed)
1. **Milestone file**: tick finished tasks `- [x] T012 … — done: <one-line note, evidence path>`. Never tick a task that is not done.
2. **progress.md**: append to "## Agent action log" (never edit the runner table above it):
   `- <ISO time> · <Mxx> · trial <t> · iter <i> · <what you did / which tasks / result of self-check>`
3. **state/handoff.md**: rewrite it completely. Short. What is half-done, what to do next, traps, commands that worked.
4. **state/result.json** (exact shape, last thing you write):
```json
{
  "milestone": "M03",
  "result": "progress | built | selfcheck_failed | stuck",
  "tasks_done": ["T030", "T031"],
  "open_tasks": 4,
  "selfcheck": "what you ran and the outcome",
  "notes": "one or two lines"
}
```
- `progress`: you finished some tasks and others remain.
- `built`: **every** task of your role in this milestone is ticked and the full self-check passed.
- `selfcheck_failed`: all tasks done but the full self-check fails and you could not fix it.
- `stuck`: you could not make progress (explain in notes and handoff).

## 6. Servers
Use only `scripts/backend.sh`, `scripts/frontend.sh`, `scripts/mock.sh` (they use PID files and health waits). Stop everything you started before you exit. The runner also stops leftovers.

## 7. Evidence
Put logs, exported Swagger, screenshots and result JSON under your `outputs/evidence/<Mxx>/`. Reference the paths in the milestone file and the action log.
