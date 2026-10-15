# Assumptions

Decisions taken by loops during building when `requirements/DECISIONS.md` did not settle a question.
The user reviews these; anything rejected becomes a FIX task.

Format: `- A-<role>-<Mxx>-<n> (<ISO time>): <question> → <assumption taken> (affects: <files>)`

- A-backend-dev-M01-1 (2026-10-15T03:24:00+03:00): a blank `email` in PUT /api/settings is 400 or stored as null? → stored as null (no error); displayName is trimmed before save (affects: backend/src/main/java/com/quickflow/settings/SettingsService.java)
- A-verify-M01-1 (2026-10-15T03:31:00+03:00): does the Maven local repository / wrapper distribution in `~/.m2` break constitution II ("nothing may depend on a file outside the project folder")? → treated as a build cache, like the JDK and Node which are also installed outside the project, so not a violation; M01 passes. To make it project-local, add `backend/.mvn/maven.config` with `-Dmaven.repo.local=.m2/repository`, set `MAVEN_USER_HOME` in the scripts and git-ignore it (needs a full re-download) (affects: backend/.mvn/, scripts/backend.sh)

