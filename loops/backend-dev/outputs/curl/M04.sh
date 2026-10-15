#!/usr/bin/env bash
# M04 curl suite: Learning (cards, milestones with derived status, notes, deletes). Each case names the acceptance id it proves.
# Runs after M01.sh, M02.sh and M03.sh. Creates its own data, never assumes ids.
source "$(dirname "$0")/../../../../scripts/curl-lib.sh"

case_begin "AC-M04-01 create card returns 201 NOT_STARTED with empty milestones and notes; GET returns it; unknown id 404"
req POST /api/learning-cards '{"title":"M04 Kotlin","description":"coroutines first"}'
expect_status 201
expect_json '.status == "NOT_STARTED"'
expect_json '.milestones | length == 0'
expect_json '.notes | length == 0'
CARD_ID=$(json '.id')
req GET "/api/learning-cards/$CARD_ID"
expect_status 200
expect_json ".id == $CARD_ID and .title == \"M04 Kotlin\""
expect_json '(.milestones | length == 0) and (.notes | length == 0)'
req GET /api/learning-cards/987654321
expect_status 404
expect_json '.status == 404'
expect_json '.message == "Learning card 987654321 not found"'
case_end

case_begin "AC-M04-02 blank card title returns 400 with fieldErrors on title"
req POST /api/learning-cards '{"title":"   "}'
expect_status 400
expect_json '.fieldErrors[0].field == "title"'
case_end

case_begin "AC-M04-03 add two milestones returns 201 each; card lists both"
req POST "/api/learning-cards/$CARD_ID/milestones" '{"title":"Basics","targetDate":"2026-11-30"}'
expect_status 201
expect_json '.done == false and .completedAt == null'
M1_ID=$(json '.id')
req POST "/api/learning-cards/$CARD_ID/milestones" '{"title":"Coroutines"}'
expect_status 201
M2_ID=$(json '.id')
req GET "/api/learning-cards/$CARD_ID"
expect_status 200
expect_json '.milestones | length == 2'
expect_json '.status == "NOT_STARTED"'
case_end

case_begin "AC-M04-04 complete one milestone sets completedAt; card becomes IN_PROGRESS"
req PUT "/api/learning-cards/$CARD_ID/milestones/$M1_ID" '{"title":"Basics","done":true}'
expect_status 200
expect_json '.done == true and .completedAt != null'
req GET "/api/learning-cards/$CARD_ID"
expect_status 200
expect_json '.status == "IN_PROGRESS"'
case_end

case_begin "AC-M04-05 complete both milestones; card becomes COMPLETED"
req PUT "/api/learning-cards/$CARD_ID/milestones/$M2_ID" '{"title":"Coroutines","done":true}'
expect_status 200
req GET "/api/learning-cards/$CARD_ID"
expect_status 200
expect_json '.status == "COMPLETED"'
case_end

case_begin "AC-M04-06 un-complete one milestone clears completedAt; card becomes IN_PROGRESS"
req PUT "/api/learning-cards/$CARD_ID/milestones/$M1_ID" '{"title":"Basics","done":false}'
expect_status 200
expect_json '.done == false and .completedAt == null'
req GET "/api/learning-cards/$CARD_ID"
expect_status 200
expect_json '.status == "IN_PROGRESS"'
case_end

case_begin "AC-M04-07 PUT a status while milestones exist returns 409; the status is unchanged"
req PUT "/api/learning-cards/$CARD_ID" '{"title":"M04 Kotlin","status":"COMPLETED"}'
expect_status 409
req GET "/api/learning-cards/$CARD_ID"
expect_json '.status == "IN_PROGRESS"'
case_end

case_begin "AC-M04-08 add a note returns 201 with createdAt; delete it returns 204 and it is gone"
req POST "/api/learning-cards/$CARD_ID/notes" '{"text":"Read chapter 3"}'
expect_status 201
expect_json '.text == "Read chapter 3" and .createdAt != null and .learningCardId == '"$CARD_ID"
NOTE_ID=$(json '.id')
req GET "/api/learning-cards/$CARD_ID"
expect_json '.notes | length == 1'
req DELETE "/api/learning-cards/$CARD_ID/notes/$NOTE_ID"
expect_status 204
req GET "/api/learning-cards/$CARD_ID"
expect_json '.notes | length == 0'
case_end

case_begin "AC-M04-09 milestone with blank title returns 400 with fieldErrors on title"
req POST "/api/learning-cards/$CARD_ID/milestones" '{"title":"  "}'
expect_status 400
expect_json '.fieldErrors[0].field == "title"'
case_end

case_begin "AC-M04-10 a card without milestones accepts a manual status on PUT"
req POST /api/learning-cards '{"title":"M04 Rust"}'
expect_status 201
RUST_ID=$(json '.id')
req PUT "/api/learning-cards/$RUST_ID" '{"title":"M04 Rust","status":"IN_PROGRESS"}'
expect_status 200
expect_json '.status == "IN_PROGRESS"'
case_end

case_begin "AC-M04-11 delete a milestone returns 204; the milestone is gone and the milestone of another card is 404"
req DELETE "/api/learning-cards/$CARD_ID/milestones/$M2_ID"
expect_status 204
req GET "/api/learning-cards/$CARD_ID"
expect_json '.milestones | length == 1'
req DELETE "/api/learning-cards/$RUST_ID/milestones/$M1_ID"
expect_status 404
case_end

case_begin "AC-M04-12 delete card returns 204; then GET is 404 and its milestones and notes are gone"
req POST "/api/learning-cards/$CARD_ID/notes" '{"text":"to be removed with the card"}'
expect_status 201
req DELETE "/api/learning-cards/$CARD_ID"
expect_status 204
req GET "/api/learning-cards/$CARD_ID"
expect_status 404
req PUT "/api/learning-cards/$CARD_ID/milestones/$M1_ID" '{"title":"Basics","done":true}'
expect_status 404
req GET /api/learning-cards
expect_status 200
expect_json "[.[] | select(.id == $CARD_ID)] | length == 0"
case_end

suite_end
