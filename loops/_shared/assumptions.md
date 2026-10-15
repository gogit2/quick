# Assumptions

Decisions taken by loops during building when `requirements/DECISIONS.md` did not settle a question.
The user reviews these; anything rejected becomes a FIX task.

Format: `- A-<role>-<Mxx>-<n> (<ISO time>): <question> → <assumption taken> (affects: <files>)`

- A-backend-dev-M01-1 (2026-10-15T03:24:00+03:00): a blank `email` in PUT /api/settings is 400 or stored as null? → stored as null (no error); displayName is trimmed before save (affects: backend/src/main/java/com/quickflow/settings/SettingsService.java)
- A-verify-M01-1 (2026-10-15T03:31:00+03:00): does the Maven local repository / wrapper distribution in `~/.m2` break constitution II ("nothing may depend on a file outside the project folder")? → treated as a build cache, like the JDK and Node which are also installed outside the project, so not a violation; M01 passes. To make it project-local, add `backend/.mvn/maven.config` with `-Dmaven.repo.local=.m2/repository`, set `MAVEN_USER_HOME` in the scripts and git-ignore it (needs a full re-download) (affects: backend/.mvn/, scripts/backend.sh)
- A-backend-dev-M02-1 (2026-10-15T03:40:00+03:00): does complete / archive / restore / reopen change updatedAt? → yes, every task mutation sets updatedAt (AC-M02-05 requires it for PUT; the rest follows the same rule) (affects: backend/src/main/java/com/quickflow/task/TaskService.java)
- A-backend-dev-M02-2 (2026-10-15T03:40:00+03:00): does due=OVERDUE return archived tasks, or DONE ones? → no. The filter uses the same rule as overdue (not DONE, not archived, due before today), so due=OVERDUE with archived=true returns nothing (affects: backend/src/main/java/com/quickflow/task/TaskQuery.java)
- A-backend-dev-M02-3 (2026-10-15T03:40:00+03:00): an invalid enum value in a query parameter (e.g. status=LATER) has no defined error body? → 400 "Validation failed" with fieldErrors[0] = the parameter name (new handler in GlobalExceptionHandler, avoids a 500). An invalid enum in a JSON body stays "Malformed request body" with empty fieldErrors, as in M01 (affects: backend/src/main/java/com/quickflow/common/GlobalExceptionHandler.java)
- A-backend-dev-M03-1 (2026-10-15T03:50:00+03:00): the 409 message for a second WEEKLY completion in the same week is not specified → "Habit already completed in the week starting <Monday>" (affects: backend/src/main/java/com/quickflow/habit/HabitService.java)
- A-backend-dev-M03-2 (2026-10-15T03:50:00+03:00): do the `from`/`to` bounds of GET /completions include their dates? → yes, both inclusive (affects: backend/src/main/java/com/quickflow/habit/HabitService.java)
- A-backend-dev-M03-3 (2026-10-15T03:50:00+03:00): when a habit's frequency is changed by PUT, what happens to its existing completions? → they are kept and the derived fields (done, streak, period) are recomputed with the new frequency (affects: backend/src/main/java/com/quickflow/habit/HabitService.java)

