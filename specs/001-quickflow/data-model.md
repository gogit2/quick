# Data model: QuickFlow

Entities from PRD section 9, with decisions D-1..D-15 applied. Table and column names are snake_case. Ids are `BIGINT` identity. Timestamps are stored as UTC `TIMESTAMP`; dates as `DATE`. Java DTO and entity classes use the contract schema names (see `contracts/openapi.yaml`).

## settings (V1, M01) - exactly one row
| Column | Type | Rules |
|---|---|---|
| id | BIGINT PK | always 1 |
| display_name | VARCHAR(100) NOT NULL | default `QuickFlow User` |
| email | VARCHAR(200) NULL | valid email when set |
| time_zone | VARCHAR(64) NOT NULL | IANA id, default `UTC` |
| notifications_enabled | BOOLEAN NOT NULL | default true |
| default_start_page | VARCHAR(20) NOT NULL | DASHBOARD, TASKS, HABITS, LEARNING, PLANS; default DASHBOARD |
| configured | BOOLEAN NOT NULL | false until the first PUT |

## task (V2, M02)
| Column | Type | Rules |
|---|---|---|
| id | BIGINT PK | |
| title | VARCHAR(200) NOT NULL | not blank (BR1) |
| description | VARCHAR(2000) NULL | BR2 |
| status | VARCHAR(20) NOT NULL | TODO, IN_PROGRESS, DONE (BR3, BR5), default TODO |
| priority | VARCHAR(10) NOT NULL | LOW, MEDIUM, HIGH, default MEDIUM |
| due_date | DATE NULL | calendar date |
| created_at, updated_at | TIMESTAMP NOT NULL | updated on every change |
| archived | BOOLEAN NOT NULL | default false (BR4) |

Derived: `overdue = due_date < today(userZone) AND status <> DONE AND NOT archived`. List filters: q (title contains, case-insensitive), status, priority, due (TODAY, OVERDUE, THIS_WEEK, NO_DUE_DATE), dueFrom/dueTo, archived, sort (D-13, D-14).
Transitions: any status to any status via PUT; `/complete` sets DONE; `/archive` and `/restore` toggle `archived`.

## habit and habit_completion (V3, M03)
| habit column | Type | Rules |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(150) NOT NULL | not blank (BR6) |
| description | VARCHAR(2000) NULL | |
| frequency | VARCHAR(10) NOT NULL | DAILY, WEEKLY |
| created_at | TIMESTAMP NOT NULL | |
| active | BOOLEAN NOT NULL | default true; false = deactivated |

| habit_completion column | Type | Rules |
|---|---|---|
| id | BIGINT PK | |
| habit_id | BIGINT NOT NULL FK habit ON DELETE CASCADE | |
| completion_date | DATE NOT NULL | not in the future (user zone) |
| created_at | TIMESTAMP NOT NULL | |
| | UNIQUE (habit_id, completion_date) | BR7 |

Rules: inactive habits cannot be completed (400). WEEKLY: a second completion in the same Monday-Sunday week is 409 (D-6). Derived on `Habit`: `doneForCurrentPeriod`, `currentStreak`, `totalCompletions`, `periodStart`, `periodEnd`.

## learning_card, learning_milestone, learning_note (V4, M04)
| learning_card column | Type | Rules |
|---|---|---|
| id | BIGINT PK | |
| title | VARCHAR(200) NOT NULL | not blank (BR8) |
| description | VARCHAR(2000) NULL | link, book or course name |
| status | VARCHAR(20) NOT NULL | NOT_STARTED, IN_PROGRESS, COMPLETED; derived when milestones exist (D-7) |
| created_at | TIMESTAMP NOT NULL | |

| learning_milestone column | Type | Rules |
|---|---|---|
| id | BIGINT PK | |
| learning_card_id | BIGINT NOT NULL FK ON DELETE CASCADE | BR9 |
| title | VARCHAR(200) NOT NULL | |
| done | BOOLEAN NOT NULL | default false |
| target_date | DATE NULL | |
| completed_at | TIMESTAMP NULL | set when done becomes true, cleared when false |

| learning_note column | Type | Rules |
|---|---|---|
| id | BIGINT PK | |
| learning_card_id | BIGINT NOT NULL FK ON DELETE CASCADE | |
| text | VARCHAR(4000) NOT NULL | not blank |
| created_at | TIMESTAMP NOT NULL | |

## plan and plan_item (V5, M05)
| plan column | Type | Rules |
|---|---|---|
| id | BIGINT PK | |
| title | VARCHAR(200) NOT NULL | not blank |
| estimated_duration_minutes | INT NOT NULL | positive (D-9); longer than the window is only a warning |
| start_date_time, end_date_time | TIMESTAMP NOT NULL | UTC; end > start (BR11) |
| priority_order | INT NOT NULL | positive; ties allowed (D-12) |
| created_at | TIMESTAMP NOT NULL | |
| start_notified | BOOLEAN NOT NULL | default false (D-8) |

There is **no status column**: status is computed (research R1).

| plan_item column | Type | Rules |
|---|---|---|
| id | BIGINT PK | |
| plan_id | BIGINT NOT NULL FK plan ON DELETE CASCADE | |
| source_type | VARCHAR(20) NOT NULL | TASK, HABIT, LEARNING_RESOURCE |
| source_id | BIGINT NOT NULL | polymorphic, no FK; the source must exist at creation |
| done | BOOLEAN NOT NULL | default false |
| | UNIQUE (plan_id, source_type, source_id) | no duplicate items |

Rules: at least one item at creation (BR10); deleting a source removes its items and deletes plans left empty (D-5); archived tasks stay.
Derived on `Plan`: `status`, `restSeconds`, `totalItems`, `doneItems`, `completionPercent = round-half-up(done*100/total)`, `durationExceedsWindow`, item `title` (resolved from the source).

### Plan status (pure function of `now`, `start`, `end`, `doneItems`, `totalItems`)
```
if totalItems > 0 and doneItems == totalItems -> COMPLETED
else if now >= end                            -> COMPLETED
else if now >= start                          -> IN_PROGRESS
else                                          -> NOT_STARTED
restSeconds = (status == IN_PROGRESS) ? max(0, end - now) : null
```
Groups: ACTIVE_UPCOMING = NOT_STARTED + IN_PROGRESS; COMPLETED = COMPLETED. Order: `priority_order` asc, then `start_date_time` asc.

## Relationships
```
Habit 1--N HabitCompletion        LearningCard 1--N LearningMilestone, 1--N LearningNote
Plan 1--N PlanItem  --(source_type, source_id)--> Task | Habit | LearningCard
```

## Dashboard (computed, no table)
Per `contracts/openapi.yaml` schema `Dashboard`: tasks metrics (non-archived), today's and overdue tasks, active habits with today's state, plan counts and the IN_PROGRESS plans, learning counts and milestones completed in the last 7 days.
