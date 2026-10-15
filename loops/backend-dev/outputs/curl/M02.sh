#!/usr/bin/env bash
# M02 curl suite: Tasks (create, read, update, lifecycle, search, filter, sort, overdue). Each case names the acceptance id it proves.
# Runs after M01.sh (which leaves the time zone at Europe/Berlin), so "yesterday" is computed in the saved user zone.
source "$(dirname "$0")/../../../../scripts/curl-lib.sh"

case_begin "AC-M02-01 POST title only returns 201 with status TODO, priority MEDIUM, archived=false"
req POST /api/tasks '{"title":"M02 defaults task"}'
expect_status 201
expect_json '.status == "TODO"'
expect_json '.priority == "MEDIUM"'
expect_json '.archived == false'
TASK_ID=$(json '.id')
case_end

case_begin "AC-M02-02 blank title returns 400 with fieldErrors[0].field = title"
req POST /api/tasks '{"title":"   "}'
expect_status 400
expect_json '.status == 400'
expect_json '.fieldErrors[0].field == "title"'
case_end

case_begin "AC-M02-03 title of 201 characters returns 400, title of 200 characters is accepted"
LONG_TITLE=$(printf 'a%.0s' $(seq 201))
req POST /api/tasks "{\"title\":\"$LONG_TITLE\"}"
expect_status 400
expect_json '.fieldErrors[0].field == "title"'
MAX_TITLE=$(printf 'b%.0s' $(seq 200))
req POST /api/tasks "{\"title\":\"$MAX_TITLE\"}"
expect_status 201
case_end

case_begin "AC-M02-04 GET /api/tasks/{id} returns 200; unknown id returns 404 with ApiError"
req GET "/api/tasks/$TASK_ID"
expect_status 200
expect_json ".id == $TASK_ID"
req GET /api/tasks/987654321
expect_status 404
expect_json '.status == 404'
expect_json '.message == "Task 987654321 not found"'
case_end

case_begin "AC-M02-05 PUT changes title, description, status, priority and due date; updatedAt changes"
req GET "/api/tasks/$TASK_ID"
OLD_UPDATED=$(json '.updatedAt')
req PUT "/api/tasks/$TASK_ID" '{"title":"M02 renamed","description":"details","status":"IN_PROGRESS","priority":"HIGH","dueDate":"2031-03-04"}'
expect_status 200
expect_json '.title == "M02 renamed"'
expect_json '.description == "details"'
expect_json '.status == "IN_PROGRESS"'
expect_json '.priority == "HIGH"'
expect_json '.dueDate == "2031-03-04"'
expect_json ".updatedAt != \"$OLD_UPDATED\""
case_end

case_begin "AC-M02-06 POST complete returns 200 with status DONE"
req POST "/api/tasks/$TASK_ID/complete"
expect_status 200
expect_json '.status == "DONE"'
case_end

case_begin "AC-M02-07 archive hides the task from the default list; archived=true lists it"
req POST "/api/tasks/$TASK_ID/archive"
expect_status 200
expect_json '.archived == true'
req GET /api/tasks
expect_status 200
expect_json "[.[] | select(.id == $TASK_ID)] | length == 0"
req GET "/api/tasks?archived=true"
expect_status 200
expect_json "[.[] | select(.id == $TASK_ID and .archived == true)] | length == 1"
case_end

case_begin "AC-M02-08 restore returns the task to the default list"
req POST "/api/tasks/$TASK_ID/restore"
expect_status 200
expect_json '.archived == false'
req GET /api/tasks
expect_status 200
expect_json "[.[] | select(.id == $TASK_ID)] | length == 1"
case_end

case_begin "AC-M02-09 DELETE returns 204, then GET returns 404 and the task is absent from lists"
req DELETE "/api/tasks/$TASK_ID"
expect_status 204
req GET "/api/tasks/$TASK_ID"
expect_status 404
req GET /api/tasks
expect_status 200
expect_json "[.[] | select(.id == $TASK_ID)] | length == 0"
case_end

case_begin "AC-M02-10 q search is case-insensitive and partial on title"
req POST /api/tasks '{"title":"Quarterly REPORT draft"}'
expect_status 201
SEARCH_ID=$(json '.id')
req GET "/api/tasks?q=report"
expect_status 200
expect_json "[.[] | select(.id == $SEARCH_ID)] | length == 1"
req GET "/api/tasks?q=RTERL"
expect_status 200
expect_json "[.[] | select(.id == $SEARCH_ID)] | length == 1"
case_end

case_begin "AC-M02-11 status and priority filters combine"
req POST /api/tasks '{"title":"M02 combo match","status":"IN_PROGRESS","priority":"HIGH"}'
expect_status 201
MATCH_ID=$(json '.id')
req POST /api/tasks '{"title":"M02 combo other","status":"IN_PROGRESS","priority":"LOW"}'
expect_status 201
OTHER_ID=$(json '.id')
req GET "/api/tasks?status=IN_PROGRESS&priority=HIGH"
expect_status 200
expect_json "[.[] | select(.id == $MATCH_ID)] | length == 1"
expect_json "[.[] | select(.id == $OTHER_ID)] | length == 0"
expect_json 'all(.[]; .status == "IN_PROGRESS" and .priority == "HIGH")'
case_end

case_begin "AC-M02-12 due=OVERDUE returns a task due yesterday with overdue=true; a DONE one is not overdue"
req GET /api/settings
ZONE=$(json '.timeZone')
YESTERDAY=$(TZ="$ZONE" date -d 'yesterday' +%F)
req POST /api/tasks "{\"title\":\"M02 overdue task\",\"dueDate\":\"$YESTERDAY\"}"
expect_status 201
OVERDUE_ID=$(json '.id')
req POST /api/tasks "{\"title\":\"M02 done yesterday\",\"dueDate\":\"$YESTERDAY\",\"status\":\"DONE\"}"
expect_status 201
DONE_ID=$(json '.id')
req GET "/api/tasks?due=OVERDUE"
expect_status 200
expect_json "[.[] | select(.id == $OVERDUE_ID and .overdue == true)] | length == 1"
expect_json "[.[] | select(.id == $DONE_ID)] | length == 0"
case_end

case_begin "AC-M02-13 sort=DUE_DATE_ASC orders by due date with nulls last"
req POST /api/tasks '{"title":"M02 due late","dueDate":"2031-06-02"}'
expect_status 201
req POST /api/tasks '{"title":"M02 due early","dueDate":"2031-06-01"}'
expect_status 201
req POST /api/tasks '{"title":"M02 no due date"}'
expect_status 201
req GET "/api/tasks?sort=DUE_DATE_ASC"
expect_status 200
expect_json '[.[].dueDate] == (([.[].dueDate | select(. != null)] | sort) + [.[].dueDate | select(. == null)])'
case_end

case_begin "AC-M02-14 invalid status value returns 400; description of 2001 characters returns 400"
req POST /api/tasks '{"title":"M02 bad status","status":"LATER"}'
expect_status 400
LONG_DESC=$(printf 'd%.0s' $(seq 2001))
req POST /api/tasks "{\"title\":\"M02 long description\",\"description\":\"$LONG_DESC\"}"
expect_status 400
expect_json '.fieldErrors[0].field == "description"'
case_end

suite_end
