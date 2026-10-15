#!/usr/bin/env bash
# M03 curl suite: Habits (create, complete per date, streaks, undo, deactivate, delete). Each case names the acceptance id it proves.
# Runs after M01.sh and M02.sh. "Today", "yesterday" and the week start are computed in the saved user zone (GET /api/settings).
source "$(dirname "$0")/../../../../scripts/curl-lib.sh"

req GET /api/settings
ZONE=$(json '.timeZone')
TODAY=$(TZ="$ZONE" date +%F)
YESTERDAY=$(TZ="$ZONE" date -d 'yesterday' +%F)
TWO_DAYS_AGO=$(TZ="$ZONE" date -d '2 days ago' +%F)
TOMORROW=$(TZ="$ZONE" date -d 'tomorrow' +%F)
WEEKDAY=$(TZ="$ZONE" date +%u)
WEEK_MONDAY=$(TZ="$ZONE" date -d "$TODAY -$((WEEKDAY - 1)) days" +%F)

case_begin "AC-M03-01 create a DAILY habit returns 201, active=true, streak 0; GET returns it; unknown id returns 404"
req POST /api/habits '{"name":"M03 read daily","frequency":"DAILY"}'
expect_status 201
expect_json '.active == true'
expect_json '.currentStreak == 0'
DAILY_ID=$(json '.id')
req GET "/api/habits/$DAILY_ID"
expect_status 200
expect_json ".id == $DAILY_ID"
req GET /api/habits/987654321
expect_status 404
expect_json '.status == 404'
expect_json '.message == "Habit 987654321 not found"'
case_end

case_begin "AC-M03-02 create a WEEKLY habit returns 201 with the Monday-Sunday period"
req POST /api/habits '{"name":"M03 review weekly","description":"weekly review","frequency":"WEEKLY"}'
expect_status 201
expect_json '.frequency == "WEEKLY"'
expect_json ".periodStart == \"$WEEK_MONDAY\""
WEEKLY_ID=$(json '.id')
case_end

case_begin "AC-M03-03 blank name returns 400; name of 151 characters returns 400"
req POST /api/habits '{"name":"   ","frequency":"DAILY"}'
expect_status 400
expect_json '.fieldErrors[0].field == "name"'
LONG_NAME=$(printf 'n%.0s' $(seq 151))
req POST /api/habits "{\"name\":\"$LONG_NAME\",\"frequency\":\"DAILY\"}"
expect_status 400
expect_json '.fieldErrors[0].field == "name"'
case_end

case_begin "AC-M03-04 complete for today returns 201; the habit is doneForCurrentPeriod with totalCompletions 1"
req POST "/api/habits/$DAILY_ID/completions" "{\"completionDate\":\"$TODAY\"}"
expect_status 201
expect_json ".completionDate == \"$TODAY\""
req GET "/api/habits/$DAILY_ID"
expect_status 200
expect_json '.doneForCurrentPeriod == true'
expect_json '.totalCompletions == 1'
case_end

case_begin "AC-M03-05 the same date again returns 409"
req POST "/api/habits/$DAILY_ID/completions" "{\"completionDate\":\"$TODAY\"}"
expect_status 409
expect_json ".message == \"Habit already completed for $TODAY\""
case_end

case_begin "AC-M03-06 a future date returns 400"
req POST "/api/habits/$DAILY_ID/completions" "{\"completionDate\":\"$TOMORROW\"}"
expect_status 400
expect_json '.fieldErrors[0].field == "completionDate"'
case_end

case_begin "AC-M03-07 a WEEKLY habit's second completion in the same week returns 409"
req POST "/api/habits/$WEEKLY_ID/completions" "{\"completionDate\":\"$WEEK_MONDAY\"}"
expect_status 201
req POST "/api/habits/$WEEKLY_ID/completions" "{\"completionDate\":\"$TODAY\"}"
expect_status 409
req GET "/api/habits/$WEEKLY_ID"
expect_json '.doneForCurrentPeriod == true'
case_end

case_begin "AC-M03-08 completion for yesterday raises totalCompletions and currentStreak of a daily habit"
req POST /api/habits '{"name":"M03 streak daily","frequency":"DAILY"}'
expect_status 201
STREAK_ID=$(json '.id')
req POST "/api/habits/$STREAK_ID/completions" "{\"completionDate\":\"$YESTERDAY\"}"
expect_status 201
req GET "/api/habits/$STREAK_ID"
expect_json '.totalCompletions == 1'
expect_json '.currentStreak == 1'
expect_json '.doneForCurrentPeriod == false'
req POST "/api/habits/$STREAK_ID/completions" "{\"completionDate\":\"$TODAY\"}"
expect_status 201
req GET "/api/habits/$STREAK_ID"
expect_json '.currentStreak == 2'
expect_json '.doneForCurrentPeriod == true'
case_end

case_begin "AC-M03-09 completions are listed newest first; from/to filter the list"
req POST "/api/habits/$STREAK_ID/completions" "{\"completionDate\":\"$TWO_DAYS_AGO\"}"
expect_status 201
req GET "/api/habits/$STREAK_ID/completions"
expect_status 200
expect_json "[.[].completionDate] == [\"$TODAY\", \"$YESTERDAY\", \"$TWO_DAYS_AGO\"]"
req GET "/api/habits/$STREAK_ID/completions?from=$YESTERDAY&to=$YESTERDAY"
expect_status 200
expect_json "length == 1 and .[0].completionDate == \"$YESTERDAY\""
case_end

case_begin "AC-M03-10 undo returns 204 and the habit is not done for today again"
req DELETE "/api/habits/$STREAK_ID/completions/$TODAY"
expect_status 204
req GET "/api/habits/$STREAK_ID"
expect_json '.doneForCurrentPeriod == false'
expect_json '.totalCompletions == 2'
case_end

case_begin "AC-M03-11 deactivate via PUT active=false; completing it returns 400"
req PUT "/api/habits/$STREAK_ID" '{"name":"M03 streak daily","frequency":"DAILY","active":false}'
expect_status 200
expect_json '.active == false'
req POST "/api/habits/$STREAK_ID/completions" "{\"completionDate\":\"$TODAY\"}"
expect_status 400
case_end

case_begin "AC-M03-12 delete returns 204, then 404; the completions are gone with the habit"
req DELETE "/api/habits/$STREAK_ID"
expect_status 204
req GET "/api/habits/$STREAK_ID"
expect_status 404
req GET "/api/habits/$STREAK_ID/completions"
expect_status 404
case_end

case_begin "AC-M03-13 GET /api/habits?active=false lists only inactive habits"
req POST /api/habits '{"name":"M03 paused","frequency":"DAILY"}'
expect_status 201
PAUSED_ID=$(json '.id')
req PUT "/api/habits/$PAUSED_ID" '{"name":"M03 paused","frequency":"DAILY","active":false}'
expect_status 200
req GET "/api/habits?active=false"
expect_status 200
expect_json "[.[] | select(.id == $PAUSED_ID)] | length == 1"
expect_json "[.[] | select(.id == $DAILY_ID)] | length == 0"
expect_json 'all(.[]; .active == false)'
case_end

suite_end
