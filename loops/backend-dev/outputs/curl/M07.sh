#!/usr/bin/env bash
# M07 curl suite: Hardening (full journey over HTTP, field limits, deletes remove plan items, unknown routes and
# malformed bodies, dashboard latency). Each case names the acceptance id it proves.
# Runs after M01.sh to M06.sh on the same database, so counts are compared as deltas.
source "$(dirname "$0")/../../../../scripts/curl-lib.sh"

NOW_EPOCH=$(date -u +%s)
iso_at() { date -u -d "@$(( NOW_EPOCH + $1 ))" +%Y-%m-%dT%H:%M:%SZ; }
rep() { printf "%${2}s" | tr ' ' "$1"; }

# Baseline figures; every delta below is taken against these
req GET /api/dashboard
expect_status 200
DONE0=$(json '.tasks.completed')
HABDONE0=$(json '.habits.completedToday')
PLANSDONE0=$(json '.plans.completed')
CARDS0=$(json '.learning.totalCards')

case_begin "AC-M07-01 full journey: task, habit, card with 2 milestones, plan of the three, all items ticked -> COMPLETED 100%; dashboard counts move"
req POST /api/tasks '{"title":"M07 journey task"}'
expect_status 201
J_TASK=$(json '.id')
req POST "/api/tasks/$J_TASK/complete"
expect_status 200
expect_json '.status == "DONE"'

req POST /api/habits '{"name":"M07 journey habit","frequency":"DAILY"}'
expect_status 201
J_HABIT=$(json '.id')
req POST "/api/habits/$J_HABIT/completions" '{}'
expect_status 201

req POST /api/learning-cards '{"title":"M07 journey card"}'
expect_status 201
J_CARD=$(json '.id')
req POST "/api/learning-cards/$J_CARD/milestones" '{"title":"Part 1"}'
expect_status 201
J_M1=$(json '.id')
req POST "/api/learning-cards/$J_CARD/milestones" '{"title":"Part 2"}'
expect_status 201
J_M2=$(json '.id')
req PUT "/api/learning-cards/$J_CARD/milestones/$J_M1" '{"title":"Part 1","done":true}'
expect_status 200
req PUT "/api/learning-cards/$J_CARD/milestones/$J_M2" '{"title":"Part 2","done":true}'
expect_status 200
req GET "/api/learning-cards/$J_CARD"
expect_json '.status == "COMPLETED"'

req POST /api/plans "{\"title\":\"M07 journey plan\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$(iso_at -3600)\",\"endDateTime\":\"$(iso_at 3600)\",\"priorityOrder\":1,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$J_TASK},{\"sourceType\":\"HABIT\",\"sourceId\":$J_HABIT},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$J_CARD}]}"
expect_status 201
J_PLAN=$(json '.id')
expect_json '.status == "IN_PROGRESS" and .totalItems == 3 and .doneItems == 0'
J_TASK_ITEM=$(json '.items[] | select(.sourceType == "TASK") | .id')
J_HABIT_ITEM=$(json '.items[] | select(.sourceType == "HABIT") | .id')
J_CARD_ITEM=$(json '.items[] | select(.sourceType == "LEARNING_RESOURCE") | .id')
req PUT "/api/plans/$J_PLAN/items/$J_TASK_ITEM" '{"done":true}'
expect_status 200
req PUT "/api/plans/$J_PLAN/items/$J_HABIT_ITEM" '{"done":true}'
expect_status 200
req PUT "/api/plans/$J_PLAN/items/$J_CARD_ITEM" '{"done":true}'
expect_status 200
expect_json '.status == "COMPLETED" and .completionPercent == 100 and .restSeconds == null'

req GET /api/dashboard
expect_status 200
expect_json "(.tasks.completed == $DONE0 + 1) and (.habits.completedToday == $HABDONE0 + 1) and (.plans.completed == $PLANSDONE0 + 1) and (.learning.totalCards == $CARDS0 + 1)"
case_end

case_begin "AC-M07-02 limits: task title 200 ok / 201 400; description 2000 ok / 2001 400; habit name 150 ok / 151 400"
req POST /api/tasks "{\"title\":\"$(rep a 200)\"}"
expect_status 201
req POST /api/tasks "{\"title\":\"$(rep a 201)\"}"
expect_status 400
expect_json '.fieldErrors | map(.field) | index("title") != null'
req POST /api/tasks "{\"title\":\"M07 long notes\",\"description\":\"$(rep d 2000)\"}"
expect_status 201
req POST /api/tasks "{\"title\":\"M07 longer notes\",\"description\":\"$(rep d 2001)\"}"
expect_status 400
expect_json '.fieldErrors | map(.field) | index("description") != null'
req POST /api/habits "{\"name\":\"$(rep h 150)\",\"frequency\":\"DAILY\"}"
expect_status 201
req POST /api/habits "{\"name\":\"$(rep h 151)\",\"frequency\":\"DAILY\"}"
expect_status 400
expect_json '.fieldErrors | map(.field) | index("name") != null'
case_end

case_begin "AC-M07-03 deleting a task, a habit and a learning card removes their plan items; a plan with no items is deleted"
req POST /api/tasks '{"title":"M07 cascade task"}'
expect_status 201
C_TASK=$(json '.id')
req POST /api/habits '{"name":"M07 cascade habit","frequency":"DAILY"}'
expect_status 201
C_HABIT=$(json '.id')
req POST /api/learning-cards '{"title":"M07 cascade card"}'
expect_status 201
C_CARD=$(json '.id')
req POST /api/plans "{\"title\":\"M07 cascade plan\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$(iso_at 7200)\",\"endDateTime\":\"$(iso_at 9000)\",\"priorityOrder\":3,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$C_TASK},{\"sourceType\":\"HABIT\",\"sourceId\":$C_HABIT},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$C_CARD}]}"
expect_status 201
C_PLAN=$(json '.id')
expect_json '.items | length == 3'
req DELETE "/api/tasks/$C_TASK"
expect_status 204
req GET "/api/plans/$C_PLAN"
expect_json '(.items | length == 2) and (.items | map(.sourceType) | index("TASK") == null)'
req DELETE "/api/habits/$C_HABIT"
expect_status 204
req GET "/api/plans/$C_PLAN"
expect_json '(.items | length == 1) and (.items[0].sourceType == "LEARNING_RESOURCE")'
req DELETE "/api/learning-cards/$C_CARD"
expect_status 204
req GET "/api/plans/$C_PLAN"
expect_status 404
case_end

case_begin "AC-M07-04 unknown route returns the ApiError 404 body; malformed JSON returns the ApiError 400 body"
req GET /api/no-such-route
expect_status 404
expect_json '(.status == 404) and (.path == "/api/no-such-route") and (.fieldErrors | length == 0) and (.timestamp | type == "string")'
req POST /api/tasks '{"title":'
expect_status 400
expect_json '(.status == 400) and (.message == "Malformed request body") and (.path == "/api/tasks")'
case_end

case_begin "AC-M07-05 GET /api/dashboard answers in under 500 ms"
T0=$(date +%s%3N)
req GET /api/dashboard
T1=$(date +%s%3N)
expect_status 200
DASH_MS=$(( T1 - T0 ))
echo "  dashboard took ${DASH_MS} ms"
(( DASH_MS < 500 )) || _fail "dashboard took ${DASH_MS} ms (limit 500)"
case_end

suite_end
