# Handoff - backend-dev (M04, after iter 2)

## Done
- All M04 [BE] tasks ticked (T045-T053). Milestone is `built` from the backend side.
- Iter 2 added `LearningNoteCreate`, `addNote`/`deleteNote`, `LearningController` (10 operations), `LearningServiceTest` (7), `LearningControllerTest` (10), and `outputs/curl/M04.sh` (12/12, executable).
- Iter 2 fixed `cards.flush()` in `addMilestone`/`addNote` so returned DTOs carry ids.
- Self-check (iter 2): `./mvnw -q verify` 125 tests green; curl-regression --upto M04 4 suites, 0 failed; export-swagger M04 PASS, 28 operations, no diff; server stopped. Evidence: `outputs/evidence/M04/selfcheck-t050-t053.md`, `swagger-diff.md`.
- Result: `built`.

## Next
- Nothing for backend-dev in M04. The verifier takes over (full stack, Playwright for the frontend side).

## Traps and facts
- Status rules (D-7, D-16) live in `LearningService`. `update` returns 409 for any non-null `status` while the card has milestones, even if it equals the derived status. With no milestones the hand-set status stays.
- Deleting the last milestone keeps the last derived status (for example COMPLETED). Logged as A-backend-dev-M04-2.
- `LearningCardRepository` is the only learning repository. Milestones and notes are reached through the card's collections.
- `spring.jpa.open-in-view=false`: lazy access must happen inside a `@Transactional` service method.
- New IDENTITY children need `cards.flush()` before their DTO is built, otherwise the id is null.
- Bash: the shell's cwd can drift. Use `cd` to the project root or `cd ../../backend` from `backend/src`. `${PIPESTATUS}` and shell-variable compound commands were denied by the permission check.
- Curl suites are not executable by default when newly written. Run `chmod +x` on new suites (M04.sh is now executable). `curl-regression` needs a `--fresh` server, because M01 expects an empty database.
- jq: `a | b and c | d` parses as one pipeline. Wrap each side in parentheses.

## Open concern (for the user, not fixed here)
- Maven resolves dependencies into `~/.m2` (outside the project). See A-verify-M01-1 in `loops/_shared/assumptions.md`.
- The M04 handoff from iter 1 claimed "8 DTO records" but only 7 existed. It is fixed now (9 DTOs in total with `LearningNoteCreate`).
