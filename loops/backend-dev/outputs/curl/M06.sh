#!/usr/bin/env bash
# M06 curl suite: Dashboard (shape, task/habit/plan/learning figures, consistency with the list endpoints). Each case names the acceptance id it proves.
# Runs after M01.sh to M05.sh on the same database, so figures are compared as deltas. AC-M06-01's zero check only applies when the database is empty.
source "$(dirname "$0")/../../../../scripts/curl-lib.sh"

NOW_EPOCH=$(date -u +%s)
iso_at() { date -u -d "@$(( NOW_EPOCH + $1 ))" +%Y-%m-%dT%H:%M:%SZ; }

# Baseline figures; every delta below is taken against these
req GET /api/dashboard
expect_status 200
TODAY=$(json '.date')
OVERDUE_DAY=$(date -d "$TODAY -2 days" +%Y-%m-%d)
DUE0=$(json '.tasks.dueToday'); OVER0=$(json '.tasks.overdue')
TOTAL0=$(json '.tasks.total'); DONE0=$(json '.tasks.completed'); DONET0=$(json '.tasks.completedToday')
HAB0=$(json '.habits.active'); HABDONE0=$(json '.habits.completedToday')
PLANA0=$(json '.plans.active')
CARDS0=$(json '.learning.totalCards'); CIP0=$(json '.learning.inProgress')
MTOTAL0=$(json '.learning.milestonesTotal'); MDONE0=$(json '.learning.milestonesDone')

case_begin "AC-M06-01 dashboard has the full shape; completionPercent is 0 when there are no tasks; empty database gives all zeros"
req GET /api/dashboard
expect_status 200
expect_json '(.date | type == "string") and (.timeZone | type == "string") and (.displayName | type == "string") and (.todayTasks | type == "array") and (.overdueTasks | type == "array") and (.habits.items | type == "array") and (.plans.items | type == "array") and (.learning.recentMilestones | type == "array") and (.tasks.completionPercent >= 0 and .tasks.completionPercent <= 100)'
if [[ "$(json '.tasks.total')" == "0" ]]; then
  expect_json '.tasks.completionPercent == 0 and .tasks.completed == 0 and .tasks.dueToday == 0 and .tasks.overdue == 0 and .habits.active == 0 and .plans.active == 0 and .learning.totalCards == 0'
fi
case_end

case_begin "AC-M06-02 tasks due today, overdue, no due date, one DONE, one archived: dueToday, overdue, total, completed, completionPercent correct"
req POST /api/tasks "{\"title\":\"M06 due today\",\"dueDate\":\"$TODAY\"}"
expect_status 201
req POST /api/tasks "{\"title\":\"M06 overdue\",\"dueDate\":\"$OVERDUE_DAY\"}"
expect_status 201
req POST /api/tasks '{"title":"M06 no due date"}'
expect_status 201
req POST /api/tasks '{"title":"M06 done","status":"DONE"}'
expect_status 201
req POST /api/tasks "{\"title\":\"M06 archived\",\"dueDate\":\"$TODAY\"}"
ARCHIVED_ID=$(json '.id')
req POST "/api/tasks/$ARCHIVED_ID/archive"
expect_status 200
req GET /api/dashboard
expect_status 200
# archived task is not counted: total grows by 4, not 5
expect_json "(.tasks.dueToday == $DUE0 + 1) and (.tasks.overdue == $OVER0 + 1) and (.tasks.total == $TOTAL0 + 4) and (.tasks.completed == $DONE0 + 1) and (.tasks.completedToday == $DONET0 + 1)"
# D-15: round-half-up of completed*100/total
expect_json '.tasks.completionPercent == ((.tasks.completed * 200 + .tasks.total) / (2 * .tasks.total) | floor)'
case_end

case_begin "AC-M06-03 two habits, one completed: habits.completedToday counts the done one, habits.active counts both"
req POST /api/habits '{"name":"M06 read","frequency":"DAILY"}'
expect_status 201
HABIT_DONE_ID=$(json '.id')
req POST /api/habits '{"name":"M06 walk","frequency":"DAILY"}'
expect_status 201
req POST "/api/habits/$HABIT_DONE_ID/completions" '{}'
expect_status 201
req GET /api/dashboard
expect_status 200
expect_json "(.habits.active == $HAB0 + 2) and (.habits.completedToday == $HABDONE0 + 1)"
case_end

case_begin "AC-M06-04 an IN_PROGRESS plan appears in plans.items with restSeconds and completionPercent"
req POST /api/tasks '{"title":"M06 plan task"}'
expect_status 201
PLAN_TASK_ID=$(json '.id')
req POST /api/plans "{\"title\":\"M06 running\",\"estimatedDurationMinutes\":120,\"startDateTime\":\"$(iso_at -60)\",\"endDateTime\":\"$(iso_at 7200)\",\"priorityOrder\":1,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$PLAN_TASK_ID}]}"
expect_status 201
RUN_PLAN_ID=$(json '.id')
req GET /api/dashboard
expect_status 200
expect_json "(.plans.active == $PLANA0 + 1) and any(.plans.items[]; .id == $RUN_PLAN_ID and .status == \"IN_PROGRESS\" and .restSeconds > 0 and .completionPercent == 0)"
case_end

case_begin "AC-M06-05 ticking a plan task item raises doneItems and the task is counted in tasks.completed"
req POST /api/tasks "{\"title\":\"M06 tick task\",\"dueDate\":\"$TODAY\"}"
expect_status 201
TICK_TASK_ID=$(json '.id')
req POST /api/tasks '{"title":"M06 tick second"}'
expect_status 201
SECOND_TASK_ID=$(json '.id')
req POST /api/plans "{\"title\":\"M06 two steps\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$(iso_at -60)\",\"endDateTime\":\"$(iso_at 3600)\",\"priorityOrder\":1,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$TICK_TASK_ID},{\"sourceType\":\"TASK\",\"sourceId\":$SECOND_TASK_ID}]}"
expect_status 201
TICK_PLAN_ID=$(json '.id')
TICK_ITEM_ID=$(json ".items[] | select(.sourceId == $TICK_TASK_ID) | .id")
req GET /api/dashboard
DONE_BEFORE=$(json '.tasks.completed')
req PUT "/api/plans/$TICK_PLAN_ID/items/$TICK_ITEM_ID" '{"done":true}'
expect_status 200
req GET /api/dashboard
expect_status 200
expect_json "(.tasks.completed == $DONE_BEFORE + 1) and any(.plans.items[]; .id == $TICK_PLAN_ID and .doneItems == 1) and any(.todayTasks[]; .id == $TICK_TASK_ID and .status == \"DONE\")"
case_end

case_begin "AC-M06-06 learning counts and recentMilestones after completing a milestone"
req POST /api/learning-cards '{"title":"M06 course","status":"IN_PROGRESS"}'
expect_status 201
CARD_ID=$(json '.id')
req POST "/api/learning-cards/$CARD_ID/milestones" '{"title":"M06 module 1"}'
expect_status 201
MILESTONE_ID=$(json '.id')
# a second, open milestone keeps the card IN_PROGRESS (a card with all milestones done is COMPLETED, D-7)
req POST "/api/learning-cards/$CARD_ID/milestones" '{"title":"M06 module 2"}'
expect_status 201
req PUT "/api/learning-cards/$CARD_ID/milestones/$MILESTONE_ID" '{"title":"M06 module 1","done":true}'
expect_status 200
req GET /api/dashboard
expect_status 200
expect_json "(.learning.totalCards == $CARDS0 + 1) and (.learning.inProgress == $CIP0 + 1) and (.learning.milestonesTotal == $MTOTAL0 + 2) and (.learning.milestonesDone == $MDONE0 + 1) and any(.learning.recentMilestones[]; .id == $MILESTONE_ID and .title == \"M06 module 1\" and .done == true)"
case_end

case_begin "AC-M06-07 every dashboard number equals the count from the list endpoints"
req GET /api/dashboard
expect_status 200
D_DUE=$(json '.tasks.dueToday'); D_OVER=$(json '.tasks.overdue'); D_TOTAL=$(json '.tasks.total'); D_DONE=$(json '.tasks.completed')
D_HAB=$(json '.habits.active'); D_HDONE=$(json '.habits.completedToday')
D_PA=$(json '.plans.active'); D_PU=$(json '.plans.upcoming'); D_PC=$(json '.plans.completed')
D_CARDS=$(json '.learning.totalCards'); D_MT=$(json '.learning.milestonesTotal'); D_MD=$(json '.learning.milestonesDone')
req GET "/api/tasks?due=TODAY"
expect_status 200
expect_json "length == $D_DUE"
req GET "/api/tasks?due=OVERDUE"
expect_status 200
expect_json "length == $D_OVER"
req GET "/api/tasks?archived=false"
expect_status 200
expect_json "(length == $D_TOTAL) and ([.[] | select(.status == \"DONE\")] | length == $D_DONE)"
req GET "/api/habits?active=true"
expect_status 200
expect_json "(length == $D_HAB) and ([.[] | select(.doneForCurrentPeriod)] | length == $D_HDONE)"
req GET /api/plans
expect_status 200
expect_json "([.[] | select(.status == \"IN_PROGRESS\")] | length == $D_PA) and ([.[] | select(.status == \"NOT_STARTED\")] | length == $D_PU) and ([.[] | select(.status == \"COMPLETED\")] | length == $D_PC)"
req GET /api/learning-cards
expect_status 200
expect_json "(length == $D_CARDS) and ([.[].milestones[]] | length == $D_MT) and ([.[].milestones[] | select(.done)] | length == $D_MD)"
case_end

suite_end
