#!/usr/bin/env bash
# M05 curl suite: Plans (create from tasks, habits and cards, computed status and rest time, item ticks, groups, cleanup). Each case names the acceptance id it proves.
# Runs after M01.sh to M04.sh. Creates its own data, never assumes ids. Plan times are computed from `date -u` at run time.
source "$(dirname "$0")/../../../../scripts/curl-lib.sh"

NOW_EPOCH=$(date -u +%s)
iso_at() { date -u -d "@$(( NOW_EPOCH + $1 ))" +%Y-%m-%dT%H:%M:%SZ; }
TODAY=$(date -u +%Y-%m-%d)

# Shared fixtures: one task, one habit, one learning card
req POST /api/tasks '{"title":"M05 write report"}'
TASK_ID=$(json '.id')
req POST /api/habits '{"name":"M05 read","frequency":"DAILY"}'
HABIT_ID=$(json '.id')
req POST /api/learning-cards '{"title":"M05 Kotlin course"}'
CARD_ID=$(json '.id')

FUT_START=$(iso_at 3600); FUT_END=$(iso_at 7200)
RUN_START=$(iso_at -2); RUN_END=$(iso_at 3600)
PAST_START=$(iso_at -7200); PAST_END=$(iso_at -3600)

case_begin "AC-M05-01 plan with a task, habit and card in the future returns 201 NOT_STARTED with no restSeconds; GET returns it; unknown id 404"
req POST /api/plans "{\"title\":\"M05 future\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$FUT_START\",\"endDateTime\":\"$FUT_END\",\"priorityOrder\":2,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$TASK_ID},{\"sourceType\":\"HABIT\",\"sourceId\":$HABIT_ID},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$CARD_ID}]}"
expect_status 201
expect_json '.status == "NOT_STARTED" and .restSeconds == null and .totalItems == 3 and .doneItems == 0'
expect_json '.startNotified == false and .durationExceedsWindow == false'
FUT_ID=$(json '.id')
req GET "/api/plans/$FUT_ID"
expect_status 200
expect_json ".id == $FUT_ID and (.items | length == 3)"
req GET /api/plans/987654321
expect_status 404
expect_json '.status == 404'
case_end

case_begin "AC-M05-02 plan with no items returns 400 with fieldErrors on items"
req POST /api/plans "{\"title\":\"M05 empty\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$FUT_START\",\"endDateTime\":\"$FUT_END\",\"priorityOrder\":1,\"items\":[]}"
expect_status 400
expect_json '.fieldErrors | map(.field) | index("items") != null'
case_end

case_begin "AC-M05-03 end before start returns 400 with fieldErrors on endDateTime"
req POST /api/plans "{\"title\":\"M05 backwards\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$FUT_END\",\"endDateTime\":\"$FUT_START\",\"priorityOrder\":1,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$TASK_ID}]}"
expect_status 400
expect_json '.fieldErrors | map(.field) | index("endDateTime") != null'
case_end

case_begin "AC-M05-04 unknown source returns 400 with fieldErrors on items"
req POST /api/plans "{\"title\":\"M05 ghost\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$FUT_START\",\"endDateTime\":\"$FUT_END\",\"priorityOrder\":1,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":987654321}]}"
expect_status 400
expect_json '.fieldErrors | map(.field) | index("items") != null'
case_end

case_begin "AC-M05-05 plan that started 2 seconds ago returns IN_PROGRESS with restSeconds above zero"
req POST /api/plans "{\"title\":\"M05 running\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$RUN_START\",\"endDateTime\":\"$RUN_END\",\"priorityOrder\":1,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$TASK_ID},{\"sourceType\":\"HABIT\",\"sourceId\":$HABIT_ID},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$CARD_ID}]}"
expect_status 201
expect_json '.status == "IN_PROGRESS" and .restSeconds > 0'
RUN_ID=$(json '.id')
RUN_TASK_ITEM=$(json '.items[] | select(.sourceType == "TASK") | .id')
RUN_HABIT_ITEM=$(json '.items[] | select(.sourceType == "HABIT") | .id')
RUN_CARD_ITEM=$(json '.items[] | select(.sourceType == "LEARNING_RESOURCE") | .id')
case_end

case_begin "AC-M05-06 ticking the task item gives doneItems 1, completionPercent 33 and sets the task DONE"
req PUT "/api/plans/$RUN_ID/items/$RUN_TASK_ITEM" '{"done":true}'
expect_status 200
expect_json '.doneItems == 1 and .completionPercent == 33 and .totalItems == 3'
req GET "/api/tasks/$TASK_ID"
expect_status 200
expect_json '.status == "DONE"'
case_end

case_begin "AC-M05-07 un-ticking the task item sets the task TODO"
req PUT "/api/plans/$RUN_ID/items/$RUN_TASK_ITEM" '{"done":false}'
expect_status 200
expect_json '.doneItems == 0 and .completionPercent == 0'
req GET "/api/tasks/$TASK_ID"
expect_status 200
expect_json '.status == "TODO"'
case_end

case_begin "AC-M05-08 ticking the habit item creates no habit completion"
req PUT "/api/plans/$RUN_ID/items/$RUN_HABIT_ITEM" '{"done":true}'
expect_status 200
expect_json '.doneItems == 1'
req GET "/api/habits/$HABIT_ID/completions?from=$TODAY&to=$TODAY"
expect_status 200
expect_json 'length == 0'
req PUT "/api/plans/$RUN_ID/items/$RUN_HABIT_ITEM" '{"done":false}'
expect_status 200
case_end

case_begin "AC-M05-09 ticking every item gives COMPLETED with restSeconds absent"
req PUT "/api/plans/$RUN_ID/items/$RUN_TASK_ITEM" '{"done":true}'
expect_status 200
req PUT "/api/plans/$RUN_ID/items/$RUN_HABIT_ITEM" '{"done":true}'
expect_status 200
req PUT "/api/plans/$RUN_ID/items/$RUN_CARD_ITEM" '{"done":true}'
expect_status 200
expect_json '.status == "COMPLETED" and .completionPercent == 100 and .restSeconds == null'
case_end

case_begin "AC-M05-10 plan that ended in the past is accepted and returns COMPLETED with its percent"
req POST /api/plans "{\"title\":\"M05 past\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$PAST_START\",\"endDateTime\":\"$PAST_END\",\"priorityOrder\":3,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$TASK_ID}]}"
expect_status 201
expect_json '.status == "COMPLETED" and .completionPercent == 0 and .restSeconds == null'
PAST_ID=$(json '.id')
case_end

case_begin "AC-M05-11 group=ACTIVE_UPCOMING and group=COMPLETED filter the list"
req GET "/api/plans?group=ACTIVE_UPCOMING"
expect_status 200
expect_json "map(.id) | index($FUT_ID) != null and index($PAST_ID) == null and index($RUN_ID) == null"
req GET "/api/plans?group=COMPLETED"
expect_status 200
expect_json "map(.id) | index($PAST_ID) != null and index($RUN_ID) != null and index($FUT_ID) == null"
case_end

case_begin "AC-M05-12 list is ordered by priorityOrder"
req GET /api/plans
expect_status 200
expect_json '[.[].priorityOrder] == ([.[].priorityOrder] | sort)'
case_end

case_begin "AC-M05-13 acknowledge-start sets startNotified true and is idempotent"
req POST "/api/plans/$FUT_ID/acknowledge-start"
expect_status 200
expect_json '.startNotified == true'
req POST "/api/plans/$FUT_ID/acknowledge-start"
expect_status 200
expect_json '.startNotified == true'
req POST /api/plans/987654321/acknowledge-start
expect_status 404
case_end

case_begin "AC-M05-14 deleting a task removes its item; a plan left empty disappears and another plan keeps its other items"
req POST /api/tasks '{"title":"M05 solo"}'
SOLO_ID=$(json '.id')
req POST /api/tasks '{"title":"M05 other"}'
OTHER_ID=$(json '.id')
req POST /api/plans "{\"title\":\"M05 solo plan\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$FUT_START\",\"endDateTime\":\"$FUT_END\",\"priorityOrder\":4,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$SOLO_ID}]}"
expect_status 201
SOLO_PLAN_ID=$(json '.id')
req POST /api/plans "{\"title\":\"M05 mixed plan\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$FUT_START\",\"endDateTime\":\"$FUT_END\",\"priorityOrder\":4,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$SOLO_ID},{\"sourceType\":\"TASK\",\"sourceId\":$OTHER_ID}]}"
expect_status 201
MIXED_PLAN_ID=$(json '.id')
req DELETE "/api/tasks/$SOLO_ID"
expect_status 204
req GET "/api/plans/$SOLO_PLAN_ID"
expect_status 404
req GET "/api/plans/$MIXED_PLAN_ID"
expect_status 200
expect_json '.totalItems == 1 and .items[0].sourceId == '"$OTHER_ID"
case_end

case_begin "AC-M05-15 deleting a plan returns 204 and the source still exists"
req POST /api/tasks '{"title":"M05 keep me"}'
KEEP_ID=$(json '.id')
req POST /api/plans "{\"title\":\"M05 to delete\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$FUT_START\",\"endDateTime\":\"$FUT_END\",\"priorityOrder\":5,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$KEEP_ID}]}"
expect_status 201
DEL_PLAN_ID=$(json '.id')
req DELETE "/api/plans/$DEL_PLAN_ID"
expect_status 204
req GET "/api/plans/$DEL_PLAN_ID"
expect_status 404
req GET "/api/tasks/$KEEP_ID"
expect_status 200
expect_json '.title == "M05 keep me"'
case_end

case_begin "AC-M05-16 duration longer than the window is accepted with durationExceedsWindow true"
req POST /api/plans "{\"title\":\"M05 too long\",\"estimatedDurationMinutes\":90,\"startDateTime\":\"$FUT_START\",\"endDateTime\":\"$(iso_at 5400)\",\"priorityOrder\":6,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$TASK_ID}]}"
expect_status 201
expect_json '.durationExceedsWindow == true'
case_end

suite_end
